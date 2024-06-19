# Use a specific version of the Alpine base image
FROM alpine:3.20.0

ENV PROJECT_DIR=/project
ENV GRADLE_USER_HOME=$PROJECT_DIR/.gradle
WORKDIR $PROJECT_DIR

# Install specific versions of dependencies
RUN apk update && \
    apk add --no-cache \
    openjdk17=17.0.11_p9-r0 \
    git=2.45.2-r0 \
    unzip=6.0-r14 \
    curl=8.7.1-r0 \
    libc6-compat=1.1.0-r4 \
    ca-certificates=20240226-r0 \
    wget=1.24.5-r0 \
    libgcc=13.2.1_git20240309-r0

# Set JAVA_HOME environment variable
ENV JAVA_HOME=/usr/lib/jvm/java-17-openjdk
ENV PATH="$JAVA_HOME/bin:${PATH}"

# Git config
RUN git config --global --add safe.directory $PROJECT_DIR

# Verify installations
RUN java -version
RUN git --version

# Set Android SDK environment variables
ENV ANDROID_HOME=/android-sdk
ENV ANDROID_USER_HOME=/.android
ENV PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools

# Define the specific version of the Android SDK tools to download
ENV ANDROID_SDK_VERSION=11076708

# Install Android SDK tools
RUN mkdir -p $ANDROID_HOME/cmdline-tools && \
    curl -o sdk-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-${ANDROID_SDK_VERSION}_latest.zip && \
    unzip sdk-tools.zip -d $ANDROID_HOME/cmdline-tools && \
    mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest && \
    rm sdk-tools.zip

# Accept Android SDK licenses
RUN yes | sdkmanager --licenses

# Install essential SDK packages (e.g., platform-tools, build-tools)
RUN sdkmanager "platform-tools" "build-tools;33.0.1" "platforms;android-30"

# Verify installations
RUN adb --version

# Expose the default ADB port
EXPOSE 5555

#ENV USER=user
#ENV USER_ID=1000
#ENV GROUP_ID=1000
#
#RUN chown -R $USER_ID:$GROUP_ID $GRADLE_USER_HOME
#RUN chown -R $USER_ID:$GROUP_ID $ANDROID_HOME
#RUN chown -R $USER_ID:$GROUP_ID /project
#
#RUN addgroup -g $GROUP_ID $USER
#RUN adduser -D -u $USER_ID -G $USER $USER
#USER $USER
#RUN ./gradlew build

#USER root
#RUN rm -rf /project
#USER $USER

##RUN dpkg --add-architecture i386
##RUN apt-get update && apt-get install -y \
##        build-essential git neovim wget unzip sudo \
##        libc6:i386 libncurses5:i386 libstdc++6:i386 lib32z1 libbz2-1.0:i386 \
##        libxrender1 libxtst6 libxi6 libfreetype6 libxft2 xz-utils vim\
##        qemu qemu-kvm libvirt-bin ubuntu-vm-builder bridge-utils libnotify4 libglu1 libqt5widgets5 openjdk-8-jdk openjdk-11-jdk xvfb \
##        && \
##    apt-get clean && rm -rf /var/lib/apt/lists/* /tmp/* /var/tmp/*
#
#RUN groupadd -g 1000 -r $USER
#RUN useradd -u 1000 -g 1000 --create-home -r $USER
##RUN adduser $USER libvirt
##RUN adduser $USER kvm
#
##Change password
#RUN echo "$USER:$USER" | chpasswd
#Make sudo passwordless
#RUN echo "${USER} ALL=(ALL) NOPASSWD:ALL" > /etc/sudoers.d/90-$USER
#RUN usermod -aG sudo $USER
#RUN usermod -aG plugdev $USER
#RUN mkdir -p /androidstudio-data
#VOLUME /androidstudio-data
#RUN chown $USER:$USER /androidstudio-data
#
#RUN mkdir -p /studio-data/Android/Sdk && \
#    chown -R $USER:$USER /studio-data/Android
#
#
#RUN mkdir -p /studio-data/profile/android && \
#    chown -R $USER:$USER /studio-data/profile
#
#COPY provisioning/docker_entrypoint.sh /usr/local/bin/docker_entrypoint.sh
#COPY provisioning/ndkTests.sh /usr/local/bin/ndkTests.sh
#RUN chmod +x /usr/local/bin/*
#COPY provisioning/51-android.rules /etc/udev/rules.d/51-android.rules
#
#USER $USER
#
#WORKDIR /home/$USER
#
##Android Studio
#ARG ANDROID_STUDIO_URL=https://redirector.gvt1.com/edgedl/android/studio/ide-zips/2022.3.1.20/android-studio-2022.3.1.20-linux.tar.gz
#ARG ANDROID_STUDIO_VERSION=2022.3.1.20
#
#RUN wget "$ANDROID_STUDIO_URL" -O android-studio.tar.gz
#RUN tar xzvf android-studio.tar.gz
#RUN rm android-studio.tar.gz
#
#RUN ln -s /studio-data/profile/AndroidStudio$ANDROID_STUDIO_VERSION .AndroidStudio$ANDROID_STUDIO_VERSION
#RUN ln -s /studio-data/Android Android
#RUN ln -s /studio-data/profile/android .android
#RUN ln -s /studio-data/profile/java .java
#RUN ln -s /studio-data/profile/gradle .gradle
#ENV ANDROID_EMULATOR_USE_SYSTEM_LIBS=1
#
#WORKDIR /home/$USER
#
#ENTRYPOINT [ "/usr/local/bin/docker_entrypoint.sh" ]
