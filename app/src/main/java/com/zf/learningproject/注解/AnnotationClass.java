package com.zf.learningproject.注解;


import androidx.annotation.NonNull;

import java.lang.annotation.Annotation;

public class AnnotationClass  {



    @NonNull
    @Override
    public String toString() {
        return super.toString();
    }

    public static void main(String[] args) {
        Annotation[] annotation = AnnotationClass.class.getAnnotations();
        for (Annotation item : annotation) {

        }
    }

}
