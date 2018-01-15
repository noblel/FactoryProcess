package com.noblel.factoryprocessor.processor;

import com.google.auto.service.AutoService;
import com.noblel.factoryprocessor.annotation.Factory;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import java.io.IOException;
import java.util.*;

/**
 * 工厂注解处理器
 *
 * @author Noblel
 */
@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {

    /**
     *
     */
    private Types typeUtils;

    /**
     * 用来处理Element工具类
     * Element代表源代码中的元素,包名(),类,变量,方法
     */
    private Elements elementUtils;

    /**
     * 用来创建文件
     */
    private Filer filer;

    /**
     * 为注解处理器提供一种报告错误的消息,警告的信息和其他消息的方式。
     */
    private Messager messager;

    private Map<String, FactoryGroupedClasses> factoryClasses =
            new LinkedHashMap<String, FactoryGroupedClasses>();

    /**
     * 被注解处理工具调用，对一些工具进行初始化
     *
     * @param processingEnv 处理环境
     */
    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        typeUtils = processingEnv.getTypeUtils();
        elementUtils = processingEnv.getElementUtils();
        filer = processingEnv.getFiler();
        messager = processingEnv.getMessager();
    }

    /**
     * 指定java版本
     *
     * @return 最新的版本
     */
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    /**
     * 表示该注解处理器可以处理哪些注解
     *
     * @return 可处理注解集合
     */
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<String>();
        annotations.add(Factory.class.getCanonicalName());
        return annotations;
    }

    /**
     * 检测类是否可用，
     * 类必须是public的
     * 不能是抽象的
     * 类必须是@MemberFactory.type()指定的类型的子类或者接口的实现
     * 类必须有一个public的无参构造器
     *
     * @param item 需要检测的带注解的类
     */
    private void isValidClass(FactoryAnnotatedClass item) throws ProcessingException {
        TypeElement classElement = item.getTypeElement();
        //检测是否是public
        if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
            throw new ProcessingException(classElement, "The class %s is not public.",
                    classElement.getQualifiedName().toString());
        }

        //检测是否是abstract的
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new ProcessingException(classElement,
                    "The class %s is abstract. You can't annotate abstract classes with @%",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
        }
        TypeElement superClassElement = elementUtils.getTypeElement(item.getQualifiedFactoryGroupName());

        //检测父类是否是接口
        if (superClassElement.getKind() == ElementKind.INTERFACE) {
            //检测是否实现了接口
            if (!classElement.getInterfaces().contains(superClassElement.asType())) {
                throw new ProcessingException(classElement,
                        "The class %s annotated with @%s must implement the interface %s",
                        classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                        item.getQualifiedFactoryGroupName());
            }
        } else {
            //检测父类
            TypeElement currentClass = classElement;
            while (true) {
                TypeMirror superClassType = currentClass.getSuperclass();
                if (superClassType.getKind() == TypeKind.NONE) {
                    throw new ProcessingException(classElement,
                            "The class %s annotated with @%s must inherit from %s",
                            classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                            item.getQualifiedFactoryGroupName());
                }
                //要求的父类找到
                if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
                    break;
                }

                currentClass = (TypeElement) typeUtils.asElement(superClassType);
            }
        }

        //检测是否有空参构造器
        for (Element enclosed : classElement.getEnclosedElements()) {
            if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
                ExecutableElement constructorElement = (ExecutableElement) enclosed;
                if (constructorElement.getParameters().size() == 0
                        && constructorElement.getModifiers().contains(Modifier.PUBLIC)) {
                    //查找到空参构造器
                    return;
                }
            }
        }

        //没有找到空参构造器
        throw new ProcessingException(classElement,
                "The class %s must provide an public empty default constructor",
                classElement.getQualifiedName().toString());
    }


    /**
     * 处理和生成java代码,类似处理器的main方法
     *
     * @param annotations 可处理注解集合
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {

        try {

            //返回一个被Interface注解的的类的列表
            for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
                /*
                Element可以是类也可以是方法,变量等
                不使用element instanceof TypeElement,因为接口也是一种TypeElement
                应该用ElementKind或者配合TypeMirror使用TypeKind
                 */
                if (annotatedElement.getKind() != ElementKind.CLASS) {
                    throw new ProcessingException(annotatedElement, "Only classes can be annotated with @%s",
                            Factory.class.getSimpleName());
                }

                TypeElement typeElement = (TypeElement) annotatedElement;

                FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);

                isValidClass(annotatedClass);

                FactoryGroupedClasses factoryClass =
                        factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
                if (factoryClass == null) {
                    String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
                    factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
                    factoryClasses.put(qualifiedGroupName, factoryClass);
                }
                factoryClass.add(annotatedClass);
            }
            for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
                factoryClass.generateCode(elementUtils, filer);
            }
            factoryClasses.clear();
        } catch (ProcessingException e) {
            error(e.getElement(), e.getMessage());
        } catch (IOException e) {
            error(null, e.getMessage());
        }
        return true;
    }


    /**
     * 显示错误信息
     */
    public void error(Element e, String msg) {
        messager.printMessage(Diagnostic.Kind.ERROR, msg, e);
    }
}
