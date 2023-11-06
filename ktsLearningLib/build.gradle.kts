plugins {

//    id("org.jetbrains.kotlin.jvm")//kotlin 配置
//    id("org.jetbrains.kotlin.android")//kotlin 配置
//    id("com.android.library")//kotlin 配置
    `maven-publish`
    `groovy-gradle-plugin`
    `java-gradle-plugin`
}

group = "com.zf.ktslearninglib"
version = "0.0.1"

gradlePlugin {
    plugins {
        create("learn") {
            id = "com.zf.learning"
            implementationClass = "com.zf.ktslearninglib.Learning"
        }
    }
}

//发布配置
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = group.toString()
            artifactId = "study"
            version = version
            from(components["java"])
        }

        // 配置仓库地址
        repositories {
            maven {
                url = uri(layout.projectDirectory.dir("maven-repo"))
            }
        }
    }
}


//dependencies {
//    //noinspection GradlePluginVersion
//    implementation("com.android.tools.build:gradle: 7.4.2")
//}