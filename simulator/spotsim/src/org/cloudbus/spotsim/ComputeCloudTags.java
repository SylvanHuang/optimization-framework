package org.cloudbus.spotsim;

import org.cloudbus.cloudsim.core.EventTag;

public enum ComputeCloudTags implements EventTag {

    RUN_INSTANCE,
    CANCEL_SPOT_REQUEST,
    TERMINATE_INSTANCES,
    CHECKPOINT_INSTANCE,
    INSTANCE_CREATED,
    INSTANCE_CREATION_FAILED,
    INSTANCE_TERMINATED,
    INSTANCE_FAILED,
    NEW_JOB_ARRIVED,
    TASK_FINISHED,
    RUN_TASK,
    CANCEL_TASK,
    FAIL_INSTANCE,
    TASK_CANCELED,
    FIRE_UP_INSTANCE,
    CHANGE_INSTANCE_PRICE,
    SCHEDULING,
    RESCHEDULING,
    CHECK_ESTIMATION,
    CHECK_PENDING_BIDS,
    CHANGE_BID,
    RETRY_NONURGENT_TASK,
    RESUME_INSTANCE,
    CLIENT_DISCONNECT,
    TASK_FAILED,
    DELAY_FAILURE,
    FAIL_RESOURCE,
    DELAY_RESOURCE_FAIL;
}
