def COLOR_MAP = [
    'SUCCESS': 'good', 
    'FAILURE': 'danger',
]
pipeline{
    
    agent any
    
    environment {
        registry = "031884245292.dkr.ecr.us-east-1.amazonaws.com/flask-cicd"
        cluster = "flask-cicd"
        service = "flaskapp"
    }
    
    stages  {
        
        stage ('Checkout') {
            steps{
                checkout scmGit(branches: [[name: '*/main']], extensions: [], userRemoteConfigs: [[url: 'https://github.com/SSingha2/Flask_CICD']])
            }
        }
        
        stage ('Docker Build') {
            steps {
                script {
                    dockerImage = docker.build registry
                }
            }
        }
        
        stage ('Docker Push'){
            steps {
                script {
                     sh 'aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 031884245292.dkr.ecr.us-east-1.amazonaws.com'
                     sh 'docker push 031884245292.dkr.ecr.us-east-1.amazonaws.com/flask-cicd:latest'
                }
            }
        }
        
        stage ('Deploy to ECS') {
            steps {
                withAWS(credentials: 'AWScreds', region: 'us-east-1'){
                    sh 'aws ecs update-service --cluster ${cluster} --service ${service} --force-new-deployment'
                }
            }
        }
    }
    
    post {
        always {
            echo 'Slack Notifications.'
            slackSend channel: '#notif',
                color: COLOR_MAP[currentBuild.currentResult],
                message: "*${currentBuild.currentResult}:* Job ${env.JOB_NAME} build ${env.BUILD_NUMBER} \n More info at: ${env.BUILD_URL}"
        }
    }
}