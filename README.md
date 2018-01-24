# TestLogisticRegression

Test code to run Fresco-Logistic-Regression from Java.

To run:

- Install https://github.com/meilof/Fresco-Logistic-Regression following the instructions in README.md.
- Clone this repository in a directory alongside ``fresco`` and ``Fresco-Logistic-Regression``.
- Open ``TestLogisticRegression.iml`` with IntelliJ
- The interesting part is in ``Test.java``, which contains code to run a FRESCO program specificed as a Kotlin coroutine
- To run this using the SPDZ runtime, chooce ``Run->Run..`` and choose "Test (P1)" for party 1; directly afterwards, also run "Test (P2)".
  A  multiparty computation will now be run between the two parties.
- Equivalently. ``Test.class`` can ben run with command-line arguments
  ``-i1 -sspdz -p1:localhost:9001 -p2:localhost:9002 -D spdz.preprocessingStrategy=dummy -D spdz.maxBitLength=128`` (for player 1); and
  ``-i2 -sspdz -p1:localhost:9001 -p2:localhost:9002 -D spdz.preprocessingStrategy=dummy -D spdz.maxBitLength=128`` (for player 2).

