#!/usr/bin/env groovy

pipeline {
  agent {
    docker {
      image 'maven:3.6-jdk-11-slim'
      // Add configuration for Nexus repositories and user/group mapping for uid:gid of 1000:1000
      args "-v ${env.JENKINS_HOME}/.m2/settings.xml:/tmp/settings.xml:ro -v /etc/passwd:/etc/passwd:ro -v /etc/group:/etc/group:ro"
    }
  }
  triggers {
    // Empty string, to allow post-commit hook to notify Jenkins
    pollSCM ''
  }
  options {
    // LogRotator will keep the 20 most recents jobs and retain the artifacts only for the 2 latest
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '2'))
  }
  parameters {
    string(name: 'MAVEN_OPTIONS', defaultValue: '', description: 'Optional parameters to be added to the mvn command line')
    booleanParam(name: 'FORCE_DEPLOY', defaultValue: false, description: 'Force the execution of the deploy stage')
  }
  environment {
    MAVEN_GLOBAL_OPTIONS = "-Duser.home=${env.WORKSPACE} -s /tmp/settings.xml --batch-mode --errors -Pdelivery"
  }

  stages {
    stage('Compile') {
      environment {
        MAVEN_OPTIONS = "${env.MAVEN_GLOBAL_OPTIONS} -U ${params.MAVEN_OPTIONS}"
      }
      steps {
        // Print disk space
        sh 'df -h $WORKSPACE'
        // Run the maven build (mvn is in the PATH of the Docker image)
        sh 'mvn $MAVEN_OPTIONS clean compile'
      }
    }
    stage('Test') {
      // Do not run the tests when performing the release on master, they should have run earlier
      when {
        not { branch 'master' }
      }
      environment {
        MAVEN_OPTIONS = "${env.MAVEN_GLOBAL_OPTIONS} -PskipCompilePlugins ${params.MAVEN_OPTIONS}"
      }
      steps {
        sh 'mvn $MAVEN_OPTIONS test'
      }
    }
    stage('Package') {
      environment {
        MAVEN_OPTIONS = "${env.MAVEN_GLOBAL_OPTIONS} -PskipCompilePlugins,skipTestPlugins ${params.MAVEN_OPTIONS}"
      }
      steps {
        sh 'mvn $MAVEN_OPTIONS package'
      }
    }
    stage('Deploy') {
      when {
        anyOf {
          branch 'master'
          branch 'dev'
          buildingTag()
          expression { params.FORCE_DEPLOY }
        }
      }
      environment {
        MAVEN_OPTIONS = "${env.MAVEN_GLOBAL_OPTIONS} -PskipCompilePlugins,skipTestPlugins ${params.MAVEN_OPTIONS}"
      }
      steps {
        sh 'mvn $MAVEN_OPTIONS deploy'
      }
    }
  }
  post {
    always {
      junit testResults: '**/target/surefire-reports/TEST-*.xml', allowEmptyResults: true
      recordIssues(
        enabledForFailure: true, aggregatingResults: true,
        tools: [
          mavenConsole(),
          java(),
          checkStyle(pattern: '**/target/checkstyle-result.xml', reportEncoding: 'UTF-8'),
          pmdParser(pattern: '**/target/pmd.xml'),
          spotBugs(pattern: '**/target/spotbugsXml.xml')
        ]
      )
      publishCoverage(
        adapters: [jacocoAdapter('**/target/site/jacoco/jacoco.xml')],
        sourceFileResolver: sourceFiles('STORE_ALL_BUILD')
      )
    }
    success {
      archiveArtifacts artifacts: '**/target/*.jar', fingerprint: true
      archiveArtifacts artifacts: '**/target/*.amp', fingerprint: true, allowEmptyArchive: true
    }
    cleanup {
      sh 'mvn $MAVEN_GLOBAL_OPTIONS --quiet clean'
    }
  }
}
