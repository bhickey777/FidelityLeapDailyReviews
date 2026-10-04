# Jenkins + SonarQube Classroom Setup

This guide explains how each student team will connect its Jenkins
pipeline to the classroom SonarQube environment, run code-quality
analysis, and enforce the classroom Quality Gate.

> **Important:** Do not commit SonarQube tokens, passwords, or other
> credentials to GitHub. Store the SonarQube token in Jenkins
> Credentials as **Secret text**.

## Architecture

Each team has its own SonarQube project and project-analysis token.

  Team     SonarQube Project Key   Jenkins Credential ID
  -------- ----------------------- -----------------------
  Team 1   `team1-app`             `sonarqube-token`
  Team 2   `team2-app`             `sonarqube-token`
  Team 3   `team3-app`             `sonarqube-token`
  Team 4   `team4-app`             `sonarqube-token`

The pipeline flow is:

``` text
GitHub
  ↓
Jenkins Checkout / Build / Test
  ↓
SonarQube Analysis
  ↓
Language-specific Quality Profiles
  ↓
Classroom Quality Gate
  ↓
PASS → Continue Pipeline
FAIL → Stop Pipeline
```

## 1. SonarQube Tokens

A SonarQube analysis token authenticates Jenkins to SonarQube. It does
**not** determine which programming-language rules are run.

You do not need separate tokens for Java, Python, TypeScript/Angular,
SQL, or other languages. If your team's SonarQube project contains
multiple languages, the same project-analysis token can be used for the
project.

Each team will receive its assigned token separately. **Do not add the
token to this README, your Jenkinsfile, source code, Docker image, or
application configuration.**

## 2. Import the Classroom Quality Profiles

Quality Profiles determine which code-analysis rules SonarQube applies.
Profiles are language-specific.

The classroom profiles are located in the `sonarqube-config` materials
under the **Extras** folder in the Dailies.

Profiles may be provided for languages such as:

-   Java
-   Python
-   JavaScript / TypeScript

Import the appropriate profiles into your local SonarQube environment so
your local analysis is consistent with the classroom environment.

## 3. Quality Profiles vs. Quality Gates

  -----------------------------------------------------------------------
  Concept                             Purpose
  ----------------------------------- -----------------------------------
  **Quality Profile**                 Determines which coding rules
                                      SonarQube checks for a language.

  **Quality Gate**                    Determines whether the analyzed
                                      code passes or fails the required
                                      quality standard.

  **Analysis Token**                  Authorizes Jenkins to submit
                                      analysis to a SonarQube project.
  -----------------------------------------------------------------------

A classroom Quality Gate may include conditions such as:

-   Coverage on new code of at least 80%
-   Duplicated lines on new code of no more than 3%
-   No unacceptable new-code reliability or security issues

The exact metrics are determined by the classroom SonarQube
configuration.

## 4. Add the SonarQube Token to Jenkins

Store your assigned team token in Jenkins Credentials.

Navigate to:

``` text
Manage Jenkins
  → Credentials
  → System
  → Global credentials
  → Add Credentials
```

Create the credential using:

``` text
Kind:        Secret text
Secret:      <your team project-analysis token>
ID:          sonarqube-token
Description: SonarQube Team Analysis Token
```

The Jenkinsfile references the credential ID. The actual token should
never appear in the Jenkinsfile.

## 5. Configure the SonarQube Scanner Plugin in Jenkins

Your Jenkins environment must know how to connect to the classroom
SonarQube server.


Navigate to:

``` text
Manage Jenkins
  → Configure System
   →  From the Sonar Qube Section select 'Add SonarQube'
   →  Update with IP address for where you are running sonar qube
      using credentials you established earlier. 
```

When configuring the plugin, use the IP address for your Linux Box
and select the `sonarqube-token` credential. Name the scanner SonarScanner

## 6. Add SonarQube Analysis to the Jenkins Pipeline

### Generic SonarScanner Example


``` groovy
stage('SonarQube Analysis') {
    		steps {
        		script {
            			def scannerHome = tool 'SonarScanner'

            			withSonarQubeEnv('SonarQube') {
                			sh """
                    			echo "SonarScanner location:"
                    			echo "${scannerHome}"

						              ${scannerHome}/bin/sonar-scanner --version
                    			${scannerHome}/bin/sonar-scanner
                			"""
            			}
        		}
    		}
	  }

```

## 7. SonarQube WebHook Setup
A WebHook is needed for Jenkins to communicate back to SonarQube concerning the Quality Gate
completion. Go into the Administration menu for your SonarQube and add a WebHook with the following
URL: http://host.docker.internal:8080/sonarqube-webhook/

This is assuming you are running sonarqube as a container under docker as demonstrated in class. Name the webhook Jenkins. 

## 8. Enforce the Quality Gate

The Jenkins pipeline should wait for SonarQube to evaluate the analysis.

``` groovy
stage('Quality Gate') {
    steps {
        timeout(time: 5, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
        }
    }
}
```

With `abortPipeline: true`, Jenkins stops the pipeline when the
SonarQube Quality Gate fails.

## 9. Local SonarQube Setup

Students may run a local SonarQube environment to check their work
before submitting it through the classroom Jenkins pipeline.

For consistent results, use the same SonarQube version as the classroom
environment.

The classroom configuration can be found in this repository wiht 
the following structure:

``` text
sonarqube-config/
├── README.md
├── quality-profiles/
│   ├── java.xml
│   ├── python.xml
│   └── typescript.xml
├── scripts/
│   └── configure-sonarqube.sh
```

The Quality Profiles can be imported into a compatible local SonarQube
installation. The classroom Quality Gate can be reproduced through the
provided configuration/setup process.

The instructor SonarQube environment remains the authoritative
environment for validation and grading.

## 10. What Will Be Validated

The instructor will verify that:

-   Jenkins successfully checks out and builds your GitHub project.
-   Automated tests execute before or as part of the analysis.
-   Jenkins authenticates to SonarQube without exposing the token.
-   Analysis is submitted to the correct team's SonarQube project.
-   The expected language-specific Quality Profiles are applied.
-   The Classroom Quality Gate is evaluated.
-   A failed Quality Gate stops the pipeline when required.

## 11. Before You Submit

Confirm all of the following:

-   [ ] Jenkins can build the project.
-   [ ] Automated tests run successfully.
-   [ ] Your SonarQube token is stored in Jenkins Credentials.
-   [ ] No SonarQube token is committed to GitHub.
-   [ ] Your Jenkinsfile uses the correct SonarQube project key.
-   [ ] SonarQube analysis completes successfully.
-   [ ] Analysis appears under your team's SonarQube project.
-   [ ] The expected Quality Profiles are being used.
-   [ ] Jenkins evaluates the Quality Gate.
-   [ ] A Quality Gate failure prevents the pipeline from continuing
    when required.

## Reference Topics

For additional help, consult the SonarQube Community Build documentation
for:

-   Authentication tokens
-   Project administration
-   Jenkins integration and global setup
-   Adding analysis to a Jenkins pipeline
-   Quality Gates
-   Quality Profiles
-   Jenkins pipeline pause / `waitForQualityGate`
-   Webhooks
