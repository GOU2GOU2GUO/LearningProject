package com.zf.ktslearninglib

import org.gradle.api.DomainObjectSet
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.ExtensionContainer
import java.lang.reflect.Field
import java.util.Objects
import kotlin.reflect.jvm.internal.impl.load.kotlin.JvmType

/**
 * @description: please add a description here
 * @author: zhang_fang
 * @date: 2023/10/31 10:25
 */
class Learning : Plugin<Project> {
    override fun apply(project: Project) {
        project.afterEvaluate {
            try {
                // project.android.defaultConfig.buildConfigField("String", "xiaomi", "\"0000000000000000000\"")
                println("这是我的插件我发功了：-------- ${project.name}")

                val android = project.extensions.getByName("android")
                println(android.javaClass.name)

//                if (android is ExtensionContainer) {
//                    val defaultConfig = android.getByName("defaultConfig")
//                    println(defaultConfig)
//                }

                var defaultConfig: Any? = null


                for (item in android.javaClass.superclass.superclass.superclass.declaredFields) {
//                    if (item.name.contains("defaultConfig")) {
//                        item.isAccessible = true
//                        defaultConfig = item.get(android)
//                        break
//                    }

                    if (item.name.contains("applicationVariants")) {
                        item.isAccessible = true
                        val applicationVariants = item.get(android) as DomainObjectSet<*>
                        applicationVariants.all {
                            for (item1 in it.javaClass.declaredMethods){
                                println(item1)
                            }
                        }

                    }
                }

//                println(defaultConfig?.javaClass?.name)
//
//                defaultConfig?.javaClass?.declaredMethods
//
//
//                for (item in defaultConfig?.javaClass?.declaredFields!!) {
//                    println(item.name)
//                }
                println("这是我的插件我发功了：-------- ${project.name}")

            } catch (e: Exception) {
                println(e.toString())
            }
        }


    }
}