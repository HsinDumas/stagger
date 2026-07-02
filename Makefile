
release:
	./gradlew --no-daemon clean \
		:stagger-core:publishToMavenLocal \
		:stagger-maven-plugin:publishToMavenLocal \
		:stagger-gradle-plugin:publishToMavenLocal

# release with keyname
release-with-key:
	./gradlew --no-daemon clean \
		:stagger-core:publishToMavenLocal \
		:stagger-maven-plugin:publishToMavenLocal \
		:stagger-gradle-plugin:publishToMavenLocal

checkstyle-checkstyle:
	./gradlew --no-daemon \
		:stagger-core:checkstyleMain \
		:stagger-maven-plugin:checkstyleMain \
		:stagger-gradle-plugin:checkstyleMain

checkstyle-check:
	./gradlew --no-daemon \
		:stagger-core:checkstyleMain \
		:stagger-maven-plugin:checkstyleMain \
		:stagger-gradle-plugin:checkstyleMain

spring-javaformat-validate:
	./gradlew --no-daemon \
		:stagger-core:checkFormatMain \
		:stagger-maven-plugin:checkFormatMain \
		:stagger-gradle-plugin:checkFormatMain

spring-javaformat-apply:
	./gradlew --no-daemon \
		:stagger-core:format \
		:stagger-maven-plugin:format \
		:stagger-gradle-plugin:format

install: spring-javaformat-apply
	./gradlew --no-daemon clean build -x test