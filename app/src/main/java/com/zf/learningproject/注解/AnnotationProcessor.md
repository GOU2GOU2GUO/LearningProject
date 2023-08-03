# Annotation Processor使用须知

1.了解Annotation Processor
Annotation Processor是javac的一个工具，它用来在编译时扫描和处理注解。通过Annotation
Processor可以获取到注解和被注解对象的相关信息，然后根据注解自动生成Java代码，省去了手动编写，提高了编码效率。使用注解处理器先要了解AbstractProcessor类，这个类是一个抽象类，有四个核心方法，关于AbstractProcessor类后面再做详细解析。

2.在Android Studio使用Annotation Processor
刚接触Annotation
Processor的同学可能会遇到找不到AbstractProcessor类的问题，大概率是因为直接在Android项目里边引用了AbstractProcessor，然而由于Android平台是基于OpenJDK的，而OpenJDK中不包含Annotation
Processor的相关代码。因此，在使用Annotation Processor时，必须在新建Module时选择Java
Library，处理注解相关的代码都需要在Java Library模块下完成。我们需要看一下整个项目的结构

```
annotation模块（Java Library） 该模块存放的是我们自定义的注解，是一个Java Library

compiler模块 (Java Library) 依赖annotation模块，处理注解并自动生成代码等，同样也是Java Library。

app (Android App) 依赖compiler模块，需要使用annotationProcessor依赖compiler模块
```

有了以上知识，接下来我们就可以用一个实例来学习Annotation Processor的用法啦。

## Annotation Processor使用实例

现在，我们来引入一个工厂模式的例子。我们先来定义一个形状的接口Shape，代码如下：

```java
public interface Shape {
    void draw();
}
```

接下来定义几个形状并实现Shape接口

```java
public class Rectangle implements Shape {
    @Override
    public void draw() {
        System.out.println("Draw a Rectangle");
    }
}

public class Triangle implements Shape {
    @Override
    public void draw() {
        System.out.println("Draw a Triangle");
    }
}

public class Circle implements Shape {
    @Override
    public void draw() {
        System.out.println("Draw a circle");
    }
}
```

然后我们需要一个工厂类来创建形状，代码如下：

```java
public class ShapeFactory {
    public Shape create(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id is null!");
        }
        if ("Circle".equals(id)) {
            return new Circle();
        }
        if ("Rectangle".equals(id)) {
            return new Rectangle();
        }
        if ("Triangle".equals(id)) {
            return new Triangle();
        }
        throw new IllegalArgumentException("Unknown id = " + id);
    }
}
```

然而，在程序中，我们随时可能会添加一个新的形状。那么此时就不得不修改工厂类来适配新添加的形状了。我们在上边已经提到过使用Annotation
Processor可以帮助我们自动生成代码。那么这个工厂类是不是可以使用Annotation
Processor来自动生成呢？这样就不需要在添加新的形状后手动修改ShapeFactory类了。仅仅需要在新添加的形状类上加上一个注解，注解处理器就会在编译时根据注解信息自动生成ShapeFactory类的代码。
制定了需求目标，接下来的任务就是来实现ShapeFactory的自动生成了。我们首先在annotation模块下定义一个Factory的注解，Factory注解的Target为ElementType.TYPE表示这个注解只能用在类、接口或者枚举上。Retention指定为RetentionPolicy.CLASS。Factory注解中有两个成员，一个Class类型的type，用来表示注解的类的类型，相同的类型表示属于同一个工厂。一个String类型的id,用来表示注解的类的名称。Factory注解代码如下

```java

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface Factory {
    Class type();

    String id();
}
```

用Factory来注解形状类

```java

@Factory(id = "Rectangle", type = Shape.class)
public class Rectangle implements Shape {
    @Override
    public void draw() {
        System.out.println("Draw a Rectangle");
    }
}
//其他形状类代码类似不再贴出
```

## 声明FactorProcessor注解处理器

在factory-compiler模块下新建FactoryProcessor类并继承AbstractAnnotation类，代码如下：

```java

@AutoService(Processor.class)
public class FactoryProcessor extends AbstractProcessor {
    private Types mTypeUtils;
    private Messager mMessager;
    private Filer mFiler;
    private Elements mElementUtils;
    private Map<String, FactoryGroupedClasses> factoryClasses = new LinkedHashMap<>();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mTypeUtils = processingEnvironment.getTypeUtils();
        mMessager = processingEnvironment.getMessager();
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> annotations = new LinkedHashSet<>();
        annotations.add(Factory.class.getCanonicalName());
        return annotations;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        //  通过RoundEnvironment获取到所有被@Factory注解的对象
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
        }
        return true;
    }

    private void error(Element e, String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args), e);
    }

    private void error(String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.ERROR, String.format(msg, args));
    }

    private void info(String msg, Object... args) {
        messager.printMessage(Diagnostic.Kind.NOTE, String.format(msg, args));
    }
}
```

可以看到，在这个类上添加了@AutoService注解，它的作用是用来生成META-INF/services/javax.annotation.processing.Processor文件的，也就是我们在使用注解处理器的时候需要手动添加META-INF/services/javax.annotation.processing.Processor，而有了@AutoService后它会自动帮我们生成。AutoService是Google开发的一个库，使用时需要在factory-compiler中添加依赖，如下：

```groovy
implementation 'com.google.auto.service:auto-service:1.0-rc4'
```

接下来针对AbstractProcessor中的四个方法来做解析
init(ProcessingEnvironment processingEnvironment)
这个方法用于初始化处理器，方法中有一个ProcessingEnvironment类型的参数，ProcessingEnvironment是一个注解处理工具的集合。如Filer可以用来编写新文件，Messager可以用来打印错误信息，还有Elements是一个可以处理Element的工具类。在这里我们有必要对Element做下说明
Element是一个接口，表示一个程序元素，它可以是包、类、方法或者一个变量。Element已知的子接口有：

```
PackageElement 表示一个包程序元素。提供对有关包及其成员的信息的访问。
ExecutableElement 表示某个类或接口的方法、构造方法或初始化程序（静态或实例），包括注释类型元素。
TypeElement 表示一个类或接口程序元素。提供对有关类型及其成员的信息的访问。注意，枚举类型是一种类，而注解类型是一种接口。
VariableElement 表示一个字段、enum 常量、方法或构造方法参数、局部变量或异常参数。
```

我们可以通过一个类来认识Element元素。

```java
package com.zhpan.mannotation.factory;  //    PackageElement

public class Circle {  //  TypeElement
    private int i; //   VariableElement
    private Triangle triangle;  //  VariableElement

    public Circle() {
    } //    ExecuteableElement

    public void draw(   //  ExecuteableElement
                        String s)   //  VariableElement
    {
        System.out.println(s);
    }

    @Override
    public void draw() {    //  ExecuteableElement
        System.out.println("Draw a circle");
    }
}
```

getSupportedSourceVersion()
这个方法用来指定当前正在使用的Java版本，通常指定SourceVersion.latestSupported()。
getSupportedAnnotationTypes()
这个方法的返回值是一个Set集合，集合中指定支持的注解类型的名称（这里必须时完整的包名+类名，例如com.example.annotation.Factory），在本例中只需要处理@Factory注解，因此Set集合中只添加了@Factory，如若有其他注解，可在此处一并添加。
process(Set《? extends TypeElement》 set, RoundEnvironment roundEnvironment) 处理先前 round
产生的类型元素上的注解类型集，并返回这些注解是否由此 Processor 处理。如果返回 true，则这些注解已处理后续
Processor 无需再处理它们；如果返回 false，则这些注解未处理并且可能要求后续 Processor
处理它们。在这个方法中，我们可以校验被注解的对象是否合法，可以编写处理注解的代码，以及自动生成java文件等。这个方法也是AbstractProcessor
中的最重要的一个方法，我们的大部分逻辑都是在这个方法中完成。后边的内容也主要是逐步完善这个方法。

上述FactoryProcessor 代码中在process方法中通过roundEnv.getElementsAnnotatedWith(Factory.class)
方法已经拿到了被注解的元素的集合。正常情况下，这个集合中应该包含的是所有被Factory注解的Shape类的元素，也就是一个TypeElement。但在编写程序代码时可能有不太了解@Factory的用途而误把@Factory用在接口或者抽象类上，这是不被允许的。因此，需要在process方法中判断该元素是否是一个类，如果不是一个类元素，那么就抛出异常，终止编译。代码如下：

```groovy
@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    //  通过RoundEnvironment获取到所有被@Factory注解的对象
    for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {
        if (annotatedElement.getKind() != ElementKind.CLASS) {
            throw new ProcessingException(annotatedElement, "Only classes can be annotated with @%s",
                    Factory.class.getSimpleName());
        }

        TypeElement typeElement = (TypeElement) annotatedElement;
        FactoryAnnotatedClass annotatedClass = new FactoryAnnotatedClass(typeElement);
    }
    return true;
}
```

另外我们需要声明一个FactoryAnnotatedClass来存放annotatedElement的相关信息。FactoryAnnotatedClass代码如下：

```java
public class FactoryAnnotatedClass {
    private TypeElement mAnnotatedClassElement;
    private String mQualifiedSuperClassName;
    private String mSimpleTypeName;
    private String mId;

    public FactoryAnnotatedClass(TypeElement classElement) {
        this.mAnnotatedClassElement = classElement;
        Factory annotation = classElement.getAnnotation(Factory.class);
        mId = annotation.id();
        if (mId.length() == 0) {
            throw new IllegalArgumentException(
                    String.format("id() in @%s for class %s is null or empty! that's not allowed",
                            Factory.class.getSimpleName(), classElement.getQualifiedName().toString()));
        }
        // Get the full QualifiedTypeName
        try {  // 该类已经被编译
            Class<?> clazz = annotation.type();
            mQualifiedSuperClassName = clazz.getCanonicalName();
            mSimpleTypeName = clazz.getSimpleName();
        } catch (MirroredTypeException mte) {// 该类未被编译
            DeclaredType classTypeMirror = (DeclaredType) mte.getTypeMirror();
            TypeElement classTypeElement = (TypeElement) classTypeMirror.asElement();
            mQualifiedSuperClassName = classTypeElement.getQualifiedName().toString();
            mSimpleTypeName = classTypeElement.getSimpleName().toString();
        }
    }
}
```

为了生成合乎要求的ShapeFactory类，在生成ShapeFactory代码前需要对被Factory注解的元素进行校验，只有通过校验，符合要求了才可以生成ShapeFactory代码。根据需求，我们列出如下规则：

```
1.只有类才能被 @Factory注解 。 因为在ShapeFactory中我们需要实例化Shape对象 ， 虽然 @Factory注解声明了Target为ElementType.TYPE ， 但接口和枚举并不符合我们的要求 。
2.被 @Factory注解的类中需要有public的构造方法 ， 这样才能实例化对象 。
3.被注解的类必须是type指定的类的子类
4.id需要为String类型 ， 并且需要在相同type组中唯一
5.具有相同type的注解类会被生成在同一个工厂类中
```

根据上面的规则，我们来一步步完成校验，如下代码：

```groovy
private void checkValidClass(FactoryAnnotatedClass item) throws ProcessingException {
    // Cast to TypeElement, has more type specific methods
    TypeElement classElement = item.getTypeElement();
    // Check if it's a public class
    if (!classElement.getModifiers().contains(Modifier.PUBLIC)) {
        throw new ProcessingException(classElement, "The class %s is not public.",
                classElement.getQualifiedName().toString());
    }
    // Check if it's an abstract class
    if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
        throw new ProcessingException(classElement,
                "The class %s is abstract. You can't annotate abstract classes with @%",
                classElement.getQualifiedName().toString(), Factory.class.getSimpleName());
    }
    // Check inheritance: Class must be child class as specified in @Factory.type();
    TypeElement superClassElement = mElementUtils.getTypeElement(item.getQualifiedFactoryGroupName());
    if (superClassElement.getKind() == ElementKind.INTERFACE) {
        // Check interface implemented
        if (!classElement.getInterfaces().contains(superClassElement.asType())) {
            throw new ProcessingException(classElement,
                    "The class %s annotated with @%s must implement the interface %s",
                    classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                    item.getQualifiedFactoryGroupName());
        }
    } else {
        // Check subclassing
        TypeElement currentClass = classElement;
        while (true) {
            /**
             * getSuperclass()
             * Returns the direct superclass of this type element.
             * If this type element represents an interface or the class java.lang.Object,
             * then a NoType with kind NONE is returned.
             */
            TypeMirror superClassType = currentClass.getSuperclass();
            if (superClassType.getKind() == TypeKind.NONE) {
                // Basis class (java.lang.Object) reached, so exit
                throw new ProcessingException(classElement,
                        "The class %s annotated with @%s must inherit from %s",
                        classElement.getQualifiedName().toString(), Factory.class.getSimpleName(),
                        item.getQualifiedFactoryGroupName());
            }
            if (superClassType.toString().equals(item.getQualifiedFactoryGroupName())) {
                // Required super class found
                break;
            }
            // Moving up in inheritance tree
            currentClass = (TypeElement) mTypeUtils.asElement(superClassType);
        }
    }
    // Check if an empty public constructor is given
    for (Element enclosed : classElement.getEnclosedElements()) {
        if (enclosed.getKind() == ElementKind.CONSTRUCTOR) {
            ExecutableElement constructorElement = (ExecutableElement) enclosed;
            if (constructorElement.getParameters().size() == 0 &&
                    constructorElement.getModifiers().contains(Modifier.PUBLIC)) {
                // Found an empty constructor
                return;
            }
        }
    }
    // No empty constructor found
    throw new ProcessingException(classElement,
            "The class %s must provide an public empty default constructor",
            classElement.getQualifiedName().toString());
}
```

另外还需声明FactoryGroupedClasses来存放FactoryAnnotatedClass，并且这个类中包含了自动生成ShapeFactory类的代码。FactoryAnnotatedClass和FactoryGroupedClasses是将注解信息封装成对象方便处理。代码如下：

```java
public class FactoryGroupedClasses {
    /**
     * Will be added to the name of the generated factory class
     */
    private static final String SUFFIX = "Factory";
    private String qualifiedClassName;
    private Map<String, FactoryAnnotatedClass> itemsMap = new LinkedHashMap<>();

    public FactoryGroupedClasses(String qualifiedClassName) {
        this.qualifiedClassName = qualifiedClassName;
    }

    public void add(FactoryAnnotatedClass toInsert) {
        FactoryAnnotatedClass factoryAnnotatedClass = itemsMap.get(toInsert.getId());
        if (factoryAnnotatedClass != null) {
            throw new IdAlreadyUsedException(factoryAnnotatedClass);
        }
        itemsMap.put(toInsert.getId(), toInsert);
    }

    public void generateCode(Elements elementUtils, Filer filer) throws IOException {
        //  Generate java file
    }
}
```

## JavaPoet自动生成代码

校验完注解信息，如果符合我们制定的规则就可以来生成ShapeFactory的代码了。我们使用square公司的一个开源框架[JavaPoet](https://github.com/square/javapoet)
来生成代码。关于这个框架大家可以去github学习如何使用，这里不再详细说明。gradel中添加以下依赖：

```groovy
compile 'com.squareup:javapoet:1.11.1'
```

JavaPoet构建并自动生成ShapeFactory类的代码如下：

```groovy
 void generateCode(Elements elementUtils, Filer filer) throws IOException {
    TypeElement superClassName = elementUtils.getTypeElement(qualifiedClassName);
    String factoryClassName = superClassName.getSimpleName() + SUFFIX;
    String qualifiedFactoryClassName = qualifiedClassName + SUFFIX;
    PackageElement pkg = elementUtils.getPackageOf(superClassName);
    String packageName = pkg.isUnnamed() ? null : pkg.getQualifiedName().toString();
    MethodSpec.Builder method = MethodSpec.methodBuilder("create").addModifiers(Modifier.PUBLIC)
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
    TypeSpec typeSpec = TypeSpec.classBuilder(factoryClassName)
            .addModifiers(Modifier.PUBLIC)
            .addMethod(method.build())
            .build();
    // Write file
    JavaFile.builder(packageName, typeSpec).build().writeTo(filer);
}
```

最后，完善process方法中的代码，如下：

```groovy
@Override
public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    try {
        for (Element annotatedElement : roundEnv.getElementsAnnotatedWith(Factory.class)) {

            FactoryGroupedClasses factoryClass = factoryClasses.get(annotatedClass.getQualifiedFactoryGroupName());
            if (factoryClass == null) {
                String qualifiedGroupName = annotatedClass.getQualifiedFactoryGroupName();
                factoryClass = new FactoryGroupedClasses(qualifiedGroupName);
                factoryClasses.put(qualifiedGroupName, factoryClass);
            }
            factoryClass.add(annotatedClass);
        }
        // Generate code
        for (FactoryGroupedClasses factoryClass : factoryClasses.values()) {
            factoryClass.generateCode(mElementUtils, mFiler);
        }
        factoryClasses.clear();
    } catch (ProcessingException e) {
        error(e.getElement(), e.getMessage());
    } catch (IOException e) {
        e.printStackTrace();
    }
    return true;
}
```

到此为止，Annotation
Processor已经可以帮我们自动来生成需要的Java文件啦。接下来Build一下项目，切换到project模式下，在app–>
build–>generated–>source–>apt–>debug–>(package)–>factory下面就可以看到ShapeFactory类，如下图：
![img](https://www.pianshen.com/images/689/72669dab7219ed76af503d6b583c0951.png)

这个类并非是我们自己编写的，而是通过上面一系列骚操作自动生成来的。现在可以再添加一个形状类继承Shape并附加@Factory注解，再次编译后都自动会生成到ShapeFactory中！到这里本篇文章就告一段落了，由于本篇文章结构比较复杂且代码也比较多，项目的源码已经放在文章末尾，可作参考。