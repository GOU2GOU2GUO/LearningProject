package com.zf.ktslearninglib

import org.gradle.api.Plugin
import org.gradle.api.Project

class Learning implements Plugin<Project> {
    @Override
    void apply(Project project) {
        project.afterEvaluate {
            try {
                project.android {
                    defaultConfig {
                        buildConfigField("String", "WX_APPID", "\"wxf23050fbba359042\"")
                        buildConfigField("String", "xiaomi", "\"0000000000000000000\"")
                    }
                }
                project.android.applicationVariants.all { variant ->
                    println variant
                }
            } catch (Exception e) {
                println e
            }
        }
    }
}