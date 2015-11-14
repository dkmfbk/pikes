# Evaluation of Pikes

This folder contains scripts to reproduce the evaluation of Pikes.

## JAR generation

Before running the tests, you have to:

* create a `lib` folder into `test`;
* run `mvn package -Prelease` to generate the jars;
* copy `pikes-tintop/target/pikes-tintop-*-jar-with-dependencies.jar` to the `test/lib` folder;
* copy `pikes-rdf/target/pikes-rdf-*-tests.jar` to the `test/lib` folder;
* copy `~/.m2/repository/org/openrdf/sesame/sesame-rio-turtle/2.8.6/sesame-rio-turtle-2.8.6.jar` to the `test/lib` folder;
* copy `~/.m2/repository/org/openrdf/sesame/sesame-queryalgebra-evaluation/2.8.6/sesame-queryalgebra-evaluation-2.8.6.jar` to the `test/lib` folder;
* setup correctly the `WORDNET_HOME` and `RDFPRO_PATH` variables in `test/config.sh` (in particular, `WORDNET_HOME` should point to

Commands summary:
```
mkdir test/lib
cp pikes-tintop/target/pikes-tintop-*-jar-with-dependencies.jar test/lib/
cp pikes-rdf/target/pikes-rdf-*-tests.jar test/lib/
cp ~/.m2/repository/org/openrdf/sesame/sesame-rio-turtle/2.8.6/sesame-rio-turtle-2.8.6.jar test/lib/
cp ~/.m2/repository/org/openrdf/sesame/sesame-queryalgebra-evaluation/2.8.6/sesame-queryalgebra-evaluation-2.8.6.jar test/lib/  
```

## Run tests

```
cd test
./test.sh
```