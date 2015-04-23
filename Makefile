# Makefile for DeepDiveLogCompiler

JAR = ddlc.jar

test: $(JAR)
	scala $(JAR) examples/test6.ddl | diff -u examples/test6.expected -

$(JAR): $(wildcard *.scala)
	sbt package
	jar=(target/scala-*/*.jar); ln -sfn $${jar[0]} $(JAR)
	touch $(JAR)

