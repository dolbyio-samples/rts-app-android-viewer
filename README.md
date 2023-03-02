# Dolby.io RTS Viewer for Android

## Setup

Add the following properties (with values) into your local.properties:

The username of any working GitHub account user.
githubUsername=

# GitHub user's personal access token (PAT) with a read:packages scope.
# For more information on how to create and use a PAT:
# https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token
# https://docs.github.com/en/packages/working-with-a-github-packages-registry/working-with-the-gradle-registry
githubPat=

At the time of writing this, the requirements to the token (see the second link above) are the following:

To authenticate to a GitHub Packages registry within a GitHub Actions workflow, you can use:

- GITHUB_TOKEN to publish packages associated with the workflow repository.
- a personal access token (classic) with at least read:packages scope to install packages associated with other private repositories (which GITHUB_TOKEN can't access).
