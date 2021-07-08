import com.github.gradle.node.NodeExtension

description = "A web ui for TNoodle that uses modern technology"

plugins {
    base
    NODEJS
}

configure<NodeExtension> {
    download.set(true)
    version.set("12.18.3")
}

val yarnInstall = tasks.named("yarn_install") {
    inputs.file("package.json")
    inputs.file("yarn.lock")

    outputs.dir("node_modules")
}

val yarnBuild = tasks.named("yarn_build") {
    dependsOn(yarnInstall)

    inputs.files(fileTree("src/main").exclude("*.css"))
    inputs.dir("public")
    inputs.file("package.json")

    outputs.dir("${project.buildDir}/static")
    outputs.file("${project.buildDir}/index.html")
}

val yarnTest = tasks.named("yarn_test") {
    dependsOn(yarnBuild)
}

val yarnPrettier = tasks.named("yarn_prettier") {
    dependsOn(yarnInstall)
}

tasks.getByName("check") {
    dependsOn(yarnTest)
    dependsOn(yarnPrettier)
}

tasks.getByName("assemble") {
    dependsOn("packageReactFrontend")
}

tasks.create<Zip>("packageReactFrontend") {
    dependsOn(yarnBuild)

    archiveBaseName.set("react-new-frontend")
    archiveExtension.set("jar")

    from("${project.buildDir}") {
        include("static/**/*")
        include("*.html")
        include("*.png")
        include("robots.txt")
        include("manifest.json")

        into("wca/tnoodle-ui")
    }
}

artifacts {
    add("default", tasks["packageReactFrontend"])
}
