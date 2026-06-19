# hmpps-x-ray-body-scans-api

[![Ministry of Justice Repository Compliance Badge](https://github-community.service.justice.gov.uk/repository-standards/api/hmpps-x-ray-body-scans-api/badge?style=flat)](https://github-community.service.justice.gov.uk/repository-standards/hmpps-x-ray-body-scans-api)
[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-x-ray-body-scans-api)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://x-ray-body-scans-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)

Template github repo used for new Kotlin based projects.

# Instructions

If this is a HMPPS project then the project will be created as part of bootstrapping -
see [hmpps-project-bootstrap](https://github.com/ministryofjustice/hmpps-project-bootstrap). You are able to specify a
template application using the `github_template_repo` attribute to clone without the need to manually do this yourself
within GitHub.

This project is community managed by the mojdt `#kotlin-dev` slack channel.
Please raise any questions or queries there. Contributions welcome!

Our security policy is located [here](https://github.com/ministryofjustice/hmpps-x-ray-body-scans-api/security/policy).

Documentation to create new service is located [here](https://tech-docs.hmpps.service.justice.gov.uk/creating-new-services/).

## Creating a Cloud Platform namespace

When deploying to a new namespace, you may wish to use the
[templates project namespace](https://github.com/ministryofjustice/cloud-platform-environments/tree/main/namespaces/live.cloud-platform.service.justice.gov.uk/hmpps-templates-dev)
as the basis for your new namespace. This namespace contains both the kotlin and typescript template projects,
which is the usual way that projects are setup.

Copy this folder and update all the existing namespace references to correspond to the environment to which you're deploying.

If you only need the kotlin configuration then remove all typescript references and remove the elasticache configuration.

To ensure the correct github teams can approve releases, you will need to make changes to the configuration in `resources/service-account-github` where the appropriate team names will need to be added (based on [lines 98-100](https://github.com/ministryofjustice/cloud-platform-environments/blob/main/namespaces/live.cloud-platform.service.justice.gov.uk/hmpps-templates-dev/resources/serviceaccount-github.tf#L98) and the reference appended to the teams list below [line 112](https://github.com/ministryofjustice/cloud-platform-environments/blob/main/namespaces/live.cloud-platform.service.justice.gov.uk/hmpps-templates-dev/resources/serviceaccount-github.tf#L112)). Note: hmpps-sre is in this list to assist with deployment issues.

Submit a PR to the Cloud Platform team in [#ask-cloud-platform](https://moj.enterprise.slack.com/archives/C57UPMZLY).
Further instructions from the Cloud Platform team can be found in the [Cloud Platform User Guide](https://user-guide.cloud-platform.service.justice.gov.uk/#cloud-platform-user-guide)

## Running the application locally

The application comes with a `dev` spring profile that includes default settings for running locally. This is not
necessary when deploying to kubernetes as these values are included in the helm configuration templates -
e.g. `values-dev.yaml`.

There is also a `docker-compose.yml` that can be used to run a local instance of the template in docker and also an
instance of HMPPS Auth (required if your service calls out to other services using a token).

```bash
docker compose pull && docker compose up
```

will run the application and HMPPS Auth within a local docker instance.

### Running the application in Intellij

```bash
docker compose pull && docker compose up --scale hmpps-x-ray-body-scans-api=0
```

will just start a docker instance of HMPPS Auth. The application should then be started with a `dev` active profile
in Intellij.

### Building and running the docker image locally

The `Dockerfile` relies on the application being built first. Steps to build the docker image:
1. Build the jar files
   ```shell
   ./gradlew clean assemble
   ```
2. Copy the jar files to the base directory so that the docker build can find them
   ```shell
   cp build/libs/*.jar .
   ```
3. Build the docker image with required arguments
   ```shell
   docker build --build-arg GIT_REF=21345 --build-arg GIT_BRANCH=bob --build-arg BUILD_NUMBER=$(date '+%Y-%m-%d') .
   ```
4. Run the docker image, setting the auth url so that it starts up
   ```shell
   docker run -e HMPPS_AUTH_URL="https://sign-in-dev.hmpps.service.justice.gov.uk/auth" <sha from step 3>
   ```
