package com.noblel.factoryprocessor.processor;

import com.noblel.factoryprocessor.annotation.Factory;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.MirroredTypeException;


import org.apache.commons.lang3.StringUtils;
/**
 * @author Noblel
 */
public class FactoryAnnotatedClass {
    /**
     * 带注解的元素
     */
    private TypeElement annotatedClassElement;

    /**
     * 符合规范的父类名
     */
    private String qualifiedGroupClassName;
    private String simpleFactoryGroupName;
    private String id;

    public FactoryAnnotatedClass(TypeElement classElement) throws ProcessingException {
        annotatedClassElement = classElement;
        Factory annotation = classElement.getAnnotation(Factory.class);
        id = annotation.id();
        if (StringUtils.isEmpty(id)) {
            throw new ProcessingException(classElement,
                    "id() in @%s for class %s is null or empty! that's not allowed",
                    Factory.class.getSimpleName(), classElement.getQualifiedName().toString());
        }

        //获取type中的合法全类名
        try {
            //第三方jar包含已经编译的被@Factory注解的clas文件
            Class<?> clazz = annotation.type();
            qualifiedGroupClassName = clazz.getCanonicalName();
            simpleFactoryGroupName = clazz.getSimpleName();
        } catch (MirroredTypeException mte) {
            //没有编译会抛MirroredTypeException异常,MirroredTypeException包含一个TypeMirror,表示未编译的类
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            qualifiedGroupClassName = classTypeElement.getQualifiedName().toString();
            simpleFactoryGroupName = classTypeElement.getSimpleName().toString();
        }
    }

    public String getId() {
        return id;
    }

    public String getQualifiedFactoryGroupName() {
        return qualifiedGroupClassName;
    }

    public String getSimpleFactoryGroupName() {
        return simpleFactoryGroupName;
    }

    public TypeElement getTypeElement() {
        return annotatedClassElement;
    }


}
