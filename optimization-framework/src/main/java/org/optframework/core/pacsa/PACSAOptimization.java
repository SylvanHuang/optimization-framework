package org.optframework.core.pacsa;


import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.util.workload.WorkflowDAG;
import org.optframework.GlobalAccess;
import org.optframework.config.Config;
import org.optframework.core.*;
import org.optframework.core.sa.SimulatedAnnealingAlgorithm;
import org.optframework.core.utils.Printer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Implementation for Parallel Ant Colony leveraged by Simulated Annealing
 * optimization algorithm
 *
 *
 * @author Hamid and Hessam - hamid.faragardi.2010@gmail.com and hessam.modaberi@gmail.com
 * @since April 2018
 * */

public class PACSAOptimization implements OptimizationAlgorithm {


    protected List<Solution> outInitialSolutionList;
    protected Solution globalBestSolution;
    protected List<Solution> initialSolutionList = new ArrayList<>();

    protected double pheromoneTrailForX[][];
    protected double pheromoneTrailForY[][];
    protected double pheromoneTrailForZ[][];

    protected Workflow workflow;
    protected WorkflowDAG dag;

    protected InstanceInfo instanceInfo[];

    protected double currentBasePheromoneValue;
    protected double initialSeed;

    double xProbability[][];
    double sumOfColumnsForX [];

    double yProbability[][];
    double sumOfColumnsForY [];

    double zProbability[];
    double sumOfColumnsForZ[];

    ArrayList<Integer> usedInstances;
    boolean instanceVisited[];

    int maxNumberOfInstances;

    public PACSAOptimization(List<Solution> outInitialSolutionList, double pheromoneInitialSeed, Workflow workflow, InstanceInfo instanceInfo[], int maxNumberOfInstances) {
        this.workflow = workflow;
        this.dag = workflow.getWfDAG();
        this.instanceInfo = instanceInfo;
        this.outInitialSolutionList = outInitialSolutionList;
        this.currentBasePheromoneValue = pheromoneInitialSeed;
        this.initialSeed = pheromoneInitialSeed;
        this.usedInstances = new ArrayList<>();
        this.instanceVisited = new boolean[maxNumberOfInstances];
        this.maxNumberOfInstances = maxNumberOfInstances;

        /**
         * Pheromone trail structure:
         * rows = tasks
         * columns = number of instances + number of different types of instances
         * */
        pheromoneTrailForX = new double[maxNumberOfInstances][this.workflow.getNumberTasks()];
        for (int i = 0; i < maxNumberOfInstances; i++) {
            for (int j = 0; j < workflow.getNumberTasks(); j++) {
                pheromoneTrailForX[i][j] = pheromoneInitialSeed;
            }
        }

        pheromoneTrailForY = new double[instanceInfo.length][maxNumberOfInstances];
        for (int i = 0; i < instanceInfo.length; i++) {
            for (int j = 0; j < maxNumberOfInstances; j++) {
                pheromoneTrailForY[i][j] = pheromoneInitialSeed;
            }
        }

        pheromoneTrailForZ = new double[this.workflow.getJobList().size()][this.workflow.getJobList().size()];
        for (int i = 0; i < workflow.getJobList().size(); i++) {
            for (int j = 0; j < workflow.getJobList().size(); j++) {
                pheromoneTrailForZ[i][j] = pheromoneInitialSeed;
            }
        }
    }

    @Override
    public Solution runAlgorithm() {
        Printer.printSAInfo();

        int iteration_counter = 0;

        //This generates the random initial solutions for the PACSA algorithm
        generateRandomInitialSolutionList();

        while (Config.sa_algorithm.cooling_factor < Config.pacsa_algorithm.equilibrium_point) {

            Solution[] antSolutionList;
            if(iteration_counter == 0) {
                antSolutionList = runAnts(true);
            }else
            {
                antSolutionList = runAnts(false);
            }

            Solution bestCurrentSolution = null;
            try {
                bestCurrentSolution = antSolutionList[0].clone();
            }
            catch (Exception e)
            {
                org.optframework.core.Log.logger.info("Cloning Exception");
            }

            iteration_counter++;

            //Update the best solution
            for (Solution solution: antSolutionList){
                if (solution.fitnessValue < bestCurrentSolution.fitnessValue){
                    bestCurrentSolution = solution;
                }
            }

            if (bestCurrentSolution.fitnessValue < globalBestSolution.fitnessValue){
                globalBestSolution = bestCurrentSolution;
            }

            Solution solutionToUpdate;
            if (Config.pacsa_algorithm.global_based){
                solutionToUpdate = globalBestSolution;
            }else {
                solutionToUpdate = bestCurrentSolution;
            }

            for (int instanceId : solutionToUpdate.xArray){
                if (!instanceVisited[instanceId]){
                    usedInstances.add(instanceId);
                    instanceVisited[instanceId] = true;
                }
            }

            //updated by Hamid

            //updates x pheromone trail
            for (int i = 0; i < maxNumberOfInstances; i++) {
                for (int j = 0; j < workflow.getNumberTasks(); j++) {
                    pheromoneTrailForX[i][j] *= Config.pacsa_algorithm.evaporation_factor;
                    if(solutionToUpdate.xArray[j] == i){
                        pheromoneTrailForX[i][j] += 1 / solutionToUpdate.fitnessValue;
                    }
                }
            }

            //updates y pheromone trail
            for (int i = 0; i < instanceInfo.length; i++) {
                for (int j = 0; j < maxNumberOfInstances; j++) {
                    pheromoneTrailForY[i][j] *= Config.pacsa_algorithm.evaporation_factor;
                    if(solutionToUpdate.yArray[j] == i){
                        pheromoneTrailForY[i][j] += 1 / solutionToUpdate.fitnessValue;
                    }
                }
            }

            //updates z pheromone trail
            for (int k = 0; k < workflow.getJobList().size(); k++) {
                for (int j = 0; j < workflow.getJobList().size(); j++) {
                    pheromoneTrailForZ[j][k] *= Config.pacsa_algorithm.evaporation_factor;
                    if (j == solutionToUpdate.zArray[k]) {
                        pheromoneTrailForZ[j][k] += 1 / solutionToUpdate.fitnessValue;
                    }
                }
            }

            //update current base seed
            currentBasePheromoneValue *= Config.pacsa_algorithm.evaporation_factor;

            //Update cooling factor
            Config.sa_algorithm.cooling_factor *= Config.pacsa_algorithm.cf_increase_ratio;
                //Update initial temperature
            Config.sa_algorithm.start_temperature *= Config.pacsa_algorithm.temp_decrease_ratio;


            //prepares probability matrix for solution generation from pheromone trail
            createProbabilityMatrix();

            for (int i = 0; i < Config.pacsa_algorithm.number_of_ants; i++) {
                initialSolutionList.add(i, generateInitialSolutionFromPheromone());
            }
        }
        Log.logger.info("Pacsa Iterations="+iteration_counter);
        return globalBestSolution;
    }

    Solution[] runAnts(boolean initial_run){
        Thread threadList[] = new Thread[Config.pacsa_algorithm.getNumber_of_ants()];
        Solution[] solutionList = new Solution[Config.pacsa_algorithm.getNumber_of_ants()];

            for (int i = 0; i < Config.pacsa_algorithm.getNumber_of_ants(); i++) {
                int itr = i;
                threadList[i] = new Thread(() -> {
                    SimulatedAnnealingAlgorithm sa = new SimulatedAnnealingAlgorithm(initialSolutionList.get(itr), workflow, instanceInfo, maxNumberOfInstances);

                    Solution solution = sa.runAlgorithm();
                    solutionList[itr] = solution;
                });
            }

        for (int i = 0; i < Config.pacsa_algorithm.number_of_ants; i++) {
            threadList[i].start();
        }

        for (int i = 0; i < Config.pacsa_algorithm.number_of_ants; i++) {
            try {
                threadList[i].join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        return solutionList;
    }

    protected void generateRandomInitialSolutionList(){
        if (outInitialSolutionList != null){
            for (Solution solution: outInitialSolutionList){
                initialSolutionList.add(solution);
            }
            for (int i = 0; i < Config.pacsa_algorithm.number_of_ants - outInitialSolutionList.size(); i++) {
                Solution solution = new Solution(workflow, instanceInfo, maxNumberOfInstances);
                solution.generateFullyRandomSolution();
                solution.fitness();
                initialSolutionList.add(i , solution);
            }
        }else {
            for (int i = 0; i < Config.pacsa_algorithm.number_of_ants; i++) {
                Solution solution = new Solution(workflow, instanceInfo, maxNumberOfInstances);
                solution.generateFullyRandomSolution();
                solution.fitness();
                initialSolutionList.add(i , solution);
            }
        }
        Solution tempSolution = initialSolutionList.get(0);
        for (Solution solution : initialSolutionList){
            if (solution.fitnessValue < tempSolution.fitnessValue){
                tempSolution = solution;
            }
        }

        try {
            globalBestSolution = tempSolution.clone();
        }catch (Exception e){
            //
        }
    }

    protected void revolution(double initialSeed)
    {

        //reset x pheromone trail
        for (int i = 0; i < maxNumberOfInstances; i++) {
            for (int j = 0; j < workflow.getNumberTasks(); j++) {

                pheromoneTrailForX[i][j] = initialSeed;

            }
        }

        //reset y pheromone trail
        for (int i = 0; i < instanceInfo.length; i++) {
            for (int j = 0; j < maxNumberOfInstances; j++) {

                pheromoneTrailForY[i][j] = initialSeed;

            }
        }

        //reset z pheromone trail
        for (int k = 0; k < workflow.getJobList().size(); k++) {
            for (int j = 0; j < workflow.getJobList().size(); j++) {

                pheromoneTrailForZ[j][k] = initialSeed;

            }
        }
    }

    protected void createProbabilityMatrix(){
        this.xProbability = new double[maxNumberOfInstances][workflow.getNumberTasks()];
        this.sumOfColumnsForX = new double[workflow.getNumberTasks()];

        this.yProbability = new double[instanceInfo.length][maxNumberOfInstances];
        this.sumOfColumnsForY = new double[workflow.getNumberTasks()];

        //probability matrix for x array
        for (int i = 0; i < workflow.getNumberTasks(); i++) {
            for (Integer instanceId: usedInstances){
                sumOfColumnsForX[i] += pheromoneTrailForX[instanceId][i];
            }
            sumOfColumnsForX[i] += ((maxNumberOfInstances - usedInstances.size()) * currentBasePheromoneValue);

            if(sumOfColumnsForX[i] != 0) {
                for (Integer instanceId : usedInstances) {
                    xProbability[instanceId][i] = (pheromoneTrailForX[instanceId][i] / sumOfColumnsForX[i]);
                }
            }
            else
            {
                for (Integer instanceId : usedInstances) {
                    xProbability[instanceId][i] = 0;
                }
            }
        }

        //probability matrix for y array

        for (Integer instanceId: usedInstances){
            for (int j = 0; j < instanceInfo.length; j++) {
                sumOfColumnsForY[instanceId] += pheromoneTrailForY[j][instanceId];
            }

            if(sumOfColumnsForY[instanceId] != 0)
            {
                for (int j = 0; j < instanceInfo.length; j++) {
                    yProbability[j][instanceId] = (pheromoneTrailForY[j][instanceId] / sumOfColumnsForY[instanceId]);
                }
            }
            else
            {
                for (int j = 0; j < instanceInfo.length; j++) {
                    yProbability[j][instanceId] = 0;
                }
            }
        }
    }

    protected Solution generateInitialSolutionFromPheromone(){
      //  long CounterForTestMultiThread = 0;
        int generatedXArray[] = new int[workflow.getNumberTasks()];
        int generatedYArray[] = new int[maxNumberOfInstances];
        for (int i = 0; i < maxNumberOfInstances; i++) {
            generatedYArray[i] = -1;
        }
        Integer generatedZArray[] = new Integer[workflow.getNumberTasks()];
        Random rand = new Random();
        int maxInstances = -1;

        for (int k = 0; k < workflow.getNumberTasks(); k++) {
            double randomX = rand.nextDouble();
            double probabilitySumTemp = 0;
            int selectedInstance = -1;
            for (int i = 0; i < maxNumberOfInstances; i++) {
                if (instanceVisited[i]){
                    probabilitySumTemp += xProbability[i][k];
                }else {
                    if(sumOfColumnsForX[i] != 0) {
                        probabilitySumTemp += (currentBasePheromoneValue / sumOfColumnsForX[i]);
                    }
                    else
                    {
                        probabilitySumTemp += 0;
                    }
                }
                if (probabilitySumTemp > randomX){
                    selectedInstance = i;
                    break;
                }
            }
            generatedXArray[k] = selectedInstance;
            if (selectedInstance > maxInstances){
                maxInstances = selectedInstance;
            }
        }

        for (int instanceId=0; instanceId < maxNumberOfInstances; instanceId++){
            double randomY = rand.nextDouble();
            double probabilitySumTemp = 0;
            int selectedInstance = -1;
            if (instanceVisited[instanceId]){
            for (int i = 0; i < instanceInfo.length; i++) {

                probabilitySumTemp += yProbability[i][instanceId];

                if (probabilitySumTemp > randomY){
                    selectedInstance = i;
                    break;
                }
            }
            }else {
                Random r = new Random();
                selectedInstance = r.nextInt(instanceInfo.length);
            }
            generatedYArray[instanceId] = selectedInstance;
        }

        ArrayList<Integer> readyTasksToOrder = dag.getFirstLevel();
        int parentsSum[] = new int[workflow.getNumberTasks()];
        int numberOfParentList[] = GlobalAccess.numberOfParentsList;

        for (int k = 0; k < workflow.getNumberTasks(); k++) {
            zProbability = new double[workflow.getJobList().size()];
            sumOfColumnsForZ = new double[workflow.getNumberTasks()];

            for (Integer taskId : readyTasksToOrder){
                sumOfColumnsForZ[k] += pheromoneTrailForZ[taskId][k];
            }

            for (Integer taskIdI: readyTasksToOrder){
                if(sumOfColumnsForZ[k] != 0) {
                    zProbability[taskIdI] = (pheromoneTrailForZ[taskIdI][k] / sumOfColumnsForZ[k]);
                }
                else
                {
                    zProbability[taskIdI] = 0;
                }
            }
            int newSelectedTaskToOrder = -1;
            int idInReadyList = -1;

            double randomX = rand.nextDouble();
            double probabilitySumTemp = 0;
            for (int i = 0; i < readyTasksToOrder.size(); i++) {
                probabilitySumTemp += zProbability[readyTasksToOrder.get(i)];
                if (probabilitySumTemp > randomX){
                    newSelectedTaskToOrder = readyTasksToOrder.get(i);
                    idInReadyList = i;
                    break;
                }
            }

            if(newSelectedTaskToOrder == -1)
            {
                Random r = new Random();
                int ran = r.nextInt(readyTasksToOrder.size());
                newSelectedTaskToOrder = readyTasksToOrder.get(ran);
                idInReadyList = ran;
            }

            ArrayList<Integer> children = dag.getChildren(newSelectedTaskToOrder);
            for (int i = 0; i < children.size(); i++) {
                int childId = children.get(i);
                parentsSum[childId]++;
                if (parentsSum[childId] == numberOfParentList[childId]){
                    readyTasksToOrder.add(childId);
                }
            }

            readyTasksToOrder.remove(idInReadyList);
            generatedZArray[k] = newSelectedTaskToOrder;
        }

        Solution solution = new Solution(workflow, instanceInfo, maxNumberOfInstances);
        solution.numberOfUsedInstances = maxInstances + 1;
        solution.xArray = generatedXArray;
        solution.yArray = generatedYArray;
        solution.zArray = generatedZArray;
        solution.origin = "pacsa";
        solution.fitness();

        return solution;
    }
}
