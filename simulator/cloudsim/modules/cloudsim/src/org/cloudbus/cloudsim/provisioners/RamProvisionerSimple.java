/*
 * Title:        CloudSim Toolkit
 * Description:  CloudSim (Cloud Simulation) Toolkit for Modeling and Simulation of Clouds
 * Licence:      GPL - http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2009-2010, The University of Melbourne, Australia
 */

package org.cloudbus.cloudsim.provisioners;

import java.util.HashMap;
import java.util.Map;

import org.cloudbus.cloudsim.Vm;

/**
 * RamProvisionerSimple is an extension of RamProvisioner which uses a
 * best-effort policy to allocate memory to a VM.
 * 
 * @author Rodrigo N. Calheiros
 * @author Anton Beloglazov
 * @since CloudSim Toolkit 1.0
 */
public class RamProvisionerSimple extends RamProvisioner {

    /** The RAM table. */
    private Map<Integer, Integer> ramTable;

    /**
     * Instantiates a new ram provisioner simple.
     * 
     * @param availableRam
     *        the available ram
     */
    public RamProvisionerSimple(int availableRam) {
	super(availableRam);
	this.ramTable = new HashMap<Integer, Integer>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see cloudsim.provisioners.RamProvisioner#allocateRamForVm(cloudsim.Vm,
     * int)
     */
    @Override
    public boolean allocateRamForVm(Vm vm, int ram) {
	int maxRam = vm.getRam();

	if (ram >= maxRam) {
	    ram = maxRam;
	}

	deallocateRamForVm(vm);

	if (getAvailableRam() >= ram) {
	    setAvailableRam(getAvailableRam() - ram);
	    this.ramTable.put(vm.getId(), ram);
	    vm.setCurrentAllocatedRam(getAllocatedRamForVm(vm));
	    return true;
	}

	vm.setCurrentAllocatedRam(getAllocatedRamForVm(vm));

	return false;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * cloudsim.provisioners.RamProvisioner#getAllocatedRamForVm(cloudsim.Vm)
     */
    @Override
    public int getAllocatedRamForVm(Vm vm) {
	if (this.ramTable.containsKey(vm.getId())) {
	    return this.ramTable.get(vm.getId());
	}
	return 0;
    }

    /*
     * (non-Javadoc)
     * 
     * @see cloudsim.provisioners.RamProvisioner#deallocateRamForVm(cloudsim.Vm)
     */
    @Override
    public void deallocateRamForVm(Vm vm) {
	if (this.ramTable.containsKey(vm.getId())) {
	    int amountFreed = this.ramTable.remove(vm.getId());
	    setAvailableRam(getAvailableRam() + amountFreed);
	    vm.setCurrentAllocatedRam(0);
	}
    }

    /*
     * (non-Javadoc)
     * 
     * @see cloudsim.provisioners.RamProvisioner#deallocateRamForVm(cloudsim.Vm)
     */
    @Override
    public void deallocateRamForAllVms() {
	super.deallocateRamForAllVms();
	this.ramTable.clear();
    }

    /*
     * (non-Javadoc)
     * 
     * @see cloudsim.provisioners.RamProvisioner#isSuitableForVm(cloudsim.Vm,
     * int)
     */
    @Override
    public boolean isSuitableForVm(Vm vm, int ram) {
	int allocatedRam = getAllocatedRamForVm(vm);
	boolean result = allocateRamForVm(vm, ram);
	deallocateRamForVm(vm);
	if (allocatedRam > 0) {
	    allocateRamForVm(vm, allocatedRam);
	}
	return result;
    }
}
