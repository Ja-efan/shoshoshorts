#!/usr/bin/env groovy

import jenkins.model.*
import hudson.model.*

println "Installing Docker CLI..."
def process = "apt-get update && apt-get install -y docker.io".execute()
process.waitFor()

println "Docker CLI installation completed with exit code: ${process.exitValue()}"
if (process.exitValue() != 0) {
    println "Error installing Docker CLI: ${process.err.text}"
} else {
    println "Docker CLI installed successfully"
} 