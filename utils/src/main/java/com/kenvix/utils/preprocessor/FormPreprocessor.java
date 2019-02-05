package com.kenvix.utils.preprocessor;

import com.kenvix.utils.Environment;
import com.kenvix.utils.StringTools;
import com.kenvix.utils.annotation.form.*;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;

import static com.kenvix.utils.PreprocessorName.getFormEmptyCheckerMethodName;

public class FormPreprocessor extends BasePreprocessor {

    private void processFormNotEmpty(Element targetClass, List<Element> annotatedElements) {
        TypeMirror targetClassType = targetClass.asType();
        String targetClassFullName = getFormEmptyCheckerMethodName(targetClass.toString());

        annotatedElements.forEach(annotatedElement -> {
            if(annotatedElement.getKind() == ElementKind.FIELD) {
                TypeMirror fieldType = annotatedElement.asType();

                FormNotEmpty annotation = annotatedElement.getAnnotation(FormNotEmpty.class);

                List<MethodSpec.Builder> builders = getMethodBuilder(getFormEmptyCheckerMethodName(targetClassFullName));
                String RMemberName = StringTools.convertUppercaseLetterToUnderlinedLowercaseLetter(annotatedElement.getSimpleName().toString());
                Name fieldVarName = annotatedElement.getSimpleName();
                ClassName RId =  ClassName.get(Environment.TargetAppPackage, "R", "id");

                builders.forEach(builder -> builder
                        .addStatement("$T $N = target.findViewById($T.$N)",
                                fieldType,
                                fieldVarName,
                                RId,
                                RMemberName)
                        .beginControlFlow("if($N.getText().toString().isEmpty())", fieldVarName)
                        .addStatement("$N.setError($L) ", fieldVarName, "promptText")
                        .addStatement("return false")
                        .endControlFlow());

                FormNumberLess formNumberLess = annotatedElement.getAnnotation(FormNumberLess.class);
                FormNumberMore formNumberMore = annotatedElement.getAnnotation(FormNumberMore.class);
                FormNumberLessOrEqual formNumberLessOrEqual = annotatedElement.getAnnotation(FormNumberLessOrEqual.class);
                FormNumberMoreOrEqual formNumberMoreOrEqual = annotatedElement.getAnnotation(FormNumberMoreOrEqual.class);

                if(formNumberLess != null || formNumberLessOrEqual != null || formNumberMore != null || formNumberMoreOrEqual != null) {

                }
            }
        });
    }

    private MethodSpec.Builder getCommonFormCheckBuilder(String methodName) {
        return MethodSpec
                .methodBuilder(methodName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
                .returns(boolean.class)
                .addParameter(String.class, "promptText");
    }

    @Override
    protected List<MethodSpec.Builder> createMethodBuilder(String methodName) {
        final boolean generateCodeForViewClass = !methodName.endsWith("Activity");
        final boolean generateCodeForActivityClass = !generateCodeForViewClass;

        ClassName appCompatClass = ClassName.get("android.support.v7.app", "AppCompatActivity");
        ClassName viewClass = ClassName.get("android.view", "View");

        return new ArrayList<MethodSpec.Builder>() {{
            if(generateCodeForActivityClass)
                add(getCommonFormCheckBuilder(methodName).
                        addParameter(appCompatClass, "target"));

            if(generateCodeForViewClass)
                add(getCommonFormCheckBuilder(methodName).
                        addParameter(viewClass, "target"));
        }};
    }

    @Override
    protected boolean onProcess(Map<Element, List<Element>> filteredAnnotations, Set<? extends TypeElement> originalAnnotations, RoundEnvironment roundEnv) {
        filteredAnnotations.forEach(this::processFormNotEmpty);
        return true;
    }

    @Override
    protected Class[] getSupportedAnnotations() {
        return new Class[] {FormNotEmpty.class};
    }

    @Override
    protected boolean onProcessingOver(Map<Element, List<Element>> filteredAnnotations, Set<? extends TypeElement> originalAnnotations, RoundEnvironment roundEnv) {
        List<MethodSpec> methods = new LinkedList<>();

        getMethodBuffer().forEach((name, builderList) ->
                builderList.forEach(methodBuilder -> methods.add(methodBuilder.addStatement("return true").build()))
        );

        TypeSpec formChecker = TypeSpec.classBuilder("FormChecker")
                .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                .addMethods(methods)
                .build();

        JavaFile javaFile = JavaFile.builder(Environment.TargetAppPackage + ".generated", formChecker)
                .addFileComment(getFileHeader())
                .build();

        try {
            javaFile.writeTo(filer);
        } catch (IOException ex) {
            throw new IllegalStateException(ex.toString());
        }

        return true;
    }
}
