Build the project:
mvn clean package -DskipTests
run this:
java -Xss512m -jar target/app.jar src/test/resources/org/rebecalang/modelchecker/revision/motexample.rebeca 0 300

a dot file is generated in the root directory of the project; paste its content in https://dreampuf.github.io/GraphvizOnline/?engine=dot#
check the state space!