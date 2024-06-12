# Building Android app from existing image
```
docker compose -f docker/docker-compose.yml run smartphone-robot-android-build-env ./gradlew assemble
```

# Building Docker image
Ensure you are in the root directory of the project (not the docker subdirectory) and run the following command:
Update 0.0.1 to the desired version number.
```
docker build -f docker/Dockerfile -t topher217/smartphone-robot-android:0.0.1 .
```
