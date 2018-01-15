package com.noblel.factoryprocessor.processor;

import com.noblel.factoryprocessor.annotation.Factory;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;

import javax.annotation.processing.Filer;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 将所有FactoryAnnotatedClasses组合到一起
 *
 * @author Noblel
 */
public class FactoryGroupedClasses {

    /**
     * 类名添加一个后缀
     */
    private static final String SUFFIX = "Factory";

    private String qualifiedClassName;
    private Map<String, FactoryAnnotatedClass> itemsMap =
            new LinkedHashMap<String, FactoryAnnotatedClass>();

    public FactoryGroupedClasses(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    public void add(FactoryAnnotatedClass toInsert) throws ProcessingException {
        FactoryAnnotatedClass existing = itemsMap.get(toInsert.getId());
        if (existing != null) {
            throw new ProcessingException(toInsert.getTypeElement(),
                    "Conflict: The class %s is annotated with @%s with id ='%s' but %s already uses the same id",
                    toInsert.getTypeElement().getQualifiedName().toString(), Factory.class.getSimpleName(),
                    toInsert.getId(), existing.getTypeElement().getQualifiedName().toString());
        }

        itemsMap.put(toInsert.getId(), toInsert);

    }

    public void generateCode(Elements elementUtils, Filer filer) throws IOException {
        TypeElement superClassName = elementUtils.getTypeElement(qualifiedClassName);
        String factoryClassName = superClassName.getSimpleName() + SUFFIX;
        String qualifiedFactoryClassName = qualifiedClassName + SUFFIX;
        PackageElement pkg = elementUtils.getPackageOf(superClassName);
        String packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();

        MethodSpec.Builder method = MethodSpec.methodBuilder("create")
                .addModifiers(Modifier.PUBLIC)
                .addParameter(String.class, "id")
                .returns(TypeName.get(superClassName.asType()));

        // check if id is null
        method.beginControlFlow("if (id == null)")
                .addStatement("throw new IllegalArgumentException($S)", "id is null!")
                .endControlFlow();

        // Generate items map

        for (FactoryAnnotatedClass item : itemsMap.values()) {
            method.beginControlFlow("if ($S.equals(id))", item.getId())
                    .addStatement("return new $L()", item.getTypeElement().getQualifiedName().toString())
                    .endControlFlow();
        }

        method.addStatement("throw new IllegalArgumentException($S + id)", "Unknown id = ");

        TypeSpec typeSpec = TypeSpec.classBuilder(factoryClassName).addMethod(method.build()).build();

        // Write file
        JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
    }

//    public void generateCode(Elements elementUtils, Filer filer)
//            throws IOException {
//        TypeElement superClassName = elementUtils
//                .getTypeElement(qualifiedClassName);
//        String factoryClassName = superClassName.getSimpleName() + SUFFIX;
//
//        JavaFileObject jfo = filer
//                .createSourceFile(qualifiedClassName + SUFFIX);
//        Writer writer = jfo.openWriter();
//        JavaWriter jw = new JavaWriter(writer);
//
//        // Write package
//        PackageElement pkg = elementUtils.getPackageOf(superClassName);
//        if (!pkg.isUnnamed()) {
//            jw.emitPackage(pkg.getQualifiedName().toString());
//            jw.emitEmptyLine();
//        } else {
//            jw.emitPackage("");
//        }
//
//        jw.beginType(factoryClassName, "class", EnumSet.of(Modifier.PUBLIC));
//        jw.emitEmptyLine();
//        jw.beginMethod(qualifiedClassName, "create",
//                EnumSet.of(Modifier.PUBLIC), "String", "id");
//
//        jw.beginControlFlow("if (id == null)");
//        jw.emitStatement("throw new IllegalArgumentException(\"id is null!\")");
//        jw.endControlFlow();
//
//        for (FactoryAnnotatedClass item : itemsMap.values()) {
//            jw.beginControlFlow("if (\"%s\".equals(id))", item.getId());
//            jw.emitStatement("return new %s()", item.getTypeElement()
//                    .getQualifiedName().toString());
//            jw.endControlFlow();
//            jw.emitEmptyLine();
//        }
//        jw.emitStatement("throw new IllegalArgumentException(\"Unknown id = \" + id)");
//        jw.endMethod();
//
//        jw.endType();
//
//        jw.close();
//    }
}
