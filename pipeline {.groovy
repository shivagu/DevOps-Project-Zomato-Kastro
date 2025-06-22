pipeline {
    agent any
    tools {
        maven 'Maven3'
        jdk 'Java17'
    }
    environment{
        JENKINS_API_TOKEN = credentials("JENKINS_API_TOKEN")
    }
    stages{
        stage('clean ws'){
            steps{
            cleanWs()
            }
        }
        stage('checkout from SM'){
          steps{
            git branch: 'main', credentialsId: 'github', url: 'https://github.com/shivagu/register-app.git'
          }
        }
        stage('mvn package'){
            steps{
                sh "mvn clean package"
            }
        }
        stage('mvn clean'){
            steps{
                sh "mvn test"
            }
        }
        stage('sonarqube_analasys'){
            steps{
                withSonarQubeEnv('sonar-server'){
                    sh "mvn sonar:sonar"
                }
            }
        }
         stage("Quality Gate"){
           steps {
               script {
                    waitForQualityGate abortPipeline: false, credentialsId: 'sonar-token'
                }	
            }

        }
          stage('docker image build'){
            steps{
                sh "docker build -t  register-app-pipeline ."
            }
        }
          stage('docker push'){
            steps{
              script{
                withDockerRegistry(credentialsId: 'docker'){
                    sh "docker tag register-app-pipeline shivareddy889/register-app-pipeline:latest"
                    sh "docker push shivareddy889/register-app-pipeline:latest"
                }
              }
            }
          }
          stage("Trigger CD Pipeline") {
            steps {
                script {
                    sh "curl -v -k --user clouduser:${JENKINS_API_TOKEN} -X POST -H 'cache-control: no-cache' -H 'content-type: application/x-www-form-urlencoded'  'ec2-13-232-122-44.ap-south-1.compute.amazonaws.com:8080/job/cd/buildWithParameters?token=gitops-token'"
                }
            }
       }
    }
}
    