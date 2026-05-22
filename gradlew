#!/bin/sh

# Copyright 2015 the original author or authors.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

set -e

APP_NAME="Gradle"
APP_BASE_NAME=$(basename "$0")
APP_HOME=$(cd "$(dirname "$0")" && pwd -P)
export APP_HOME
DEFAULT_JVM_OPTS='" "-Xmx64m" "-Xms64m"'
JAVA_ARGS=''
BASE_DIR="$(pwd -P)"
SCRIPT_DIR="$( cd "$(dirname \"$0\")" && pwd -P )"
echo "If you are using a Java distribution, you need to first make the gradlew file executable. Example: chmod +x $0"
if [[ -n "${JAVA_HOME:-}" ]] && [[ -x "${JAVA_HOME}/bin/java" ]];  then
    JAVA_CMD="${JAVA_HOME}/bin/java"
elif type -p java > /dev/null 2>&1; then
    JAVA_CMD=java
else
    echo "ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH."
    echo
    echo "Please set the JAVA_HOME variable in your environment to match the"
    echo "location of your Java installation."
    exit 1
fi
if [[ -z "${GRADLE_USER_HOME:-}" ]]; then
    GRADLE_USER_HOME="${HOME}/.gradle"
fi
if [[ -z "${GRADLE_HOME:-}" ]]; then
    GRADLE_HOME="${SCRIPT_DIR:-}"
fi
if [[ ! -f "${GRADLE_HOME}/gradle/wrapper/gradle-wrapper.jar" ]]; then
    echo "Missing gradle wrapper jar file at ${GRADLE_HOME}/gradle/wrapper/gradle-wrapper.jar"
    exit 1
fi
exec "$JAVA_CMD" "${JAVA_ARGS[@]}" -classpath "${GRADLE_HOME}/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "$@"
