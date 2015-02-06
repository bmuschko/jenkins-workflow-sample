stage 'inital'

node {
    git branch: 'master', changelog: true, url: 'git@github.com:bmuschko/continuous-delivery-jump-start-solution.git'

    try {
        executeGradle('clean test', '-PcodeCoverageEnabled=true')
    } 
    finally {
        archive '**/*'
        step([$class: 'JUnitResultArchiver', keepLongStdio: false, testDataPublishers: [], testResults: '**/build/test-results/unit/*.xml'])
    }
}

stage 'integ-tests'

node {
    unarchive mapping: ['**/*' : '.']

    try {
        executeGradle('integrationTest', '-PcodeCoverageEnabled=true')
    }
    finally {
        step([$class: 'JUnitResultArchiver', keepLongStdio: false, testDataPublishers: [], testResults: '**/build/test-results/integration/*.xml'])
    }
}

stage 'code-quality'

node {
    unarchive mapping: ['**/*' : '.']
    executeGradle('findbugsMain')
}

stage 'assemble-publish-deploy'

node {
    unarchive mapping: ['**/*' : '.']
    executeGradle('uploadArchives deploy', '-Penv=local -i -x classes')
}

void setBuildNumberEnvVar() {
    env.SOURCE_BUILD_NUMBER = env.BUILD_ID
}

void executeGradle(String tasks, String switches = null) {
    setBuildNumberEnvVar()
    StringBuilder gradleCommand = new StringBuilder()
    gradleCommand <<= './gradlew '
    gradleCommand <<= tasks

    if(switches) {
        gradleCommand <<= ' '
        gradleCommand <<= switches
    }

    sh gradleCommand.toString()
}