# RinSim Dynamism Extension - Main Project v1.0 

This project houses the files required to create the datasets used and setup the experiments tested in the thesis of Vincent Van Gestel.
In the thesis a centralised algorithm is comapred with a decentralised one in the context of dynamic dial-a-ride problems.
The major extension this work provides is allowing to generate and simulate dynamic traffic events within the simulator.
This project will thus coordinate with these other projects to achieve the desired results.
Dependencies for this project include [RinSim](https://github.com/rinde/RinSim/tree/v4.4.2), [RinLog](https://github.com/rinde/RinLog/tree/develop) and the [RinSim Datset Generator](https://github.com/VincentVanGestel/pdptw-dataset-generator).
The main method of the class [ExperimentRunner](src/main/java/com/github/vincentvangestel/rinsimextension/experiment/ExperimentRunner.java) allows you to perform the aforementioned actions.
The different shell scripts in the root folder allow to execute the code with the parameters used in the thesis.
