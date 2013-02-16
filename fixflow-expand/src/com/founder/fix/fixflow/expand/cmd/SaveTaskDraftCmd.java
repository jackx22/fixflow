package com.founder.fix.fixflow.expand.cmd;


import java.util.List;


import com.founder.fix.fixflow.core.ProcessEngine;
import com.founder.fix.fixflow.core.ProcessEngineManagement;
import com.founder.fix.fixflow.core.RuntimeService;
import com.founder.fix.fixflow.core.TaskService;
import com.founder.fix.fixflow.core.exception.FixFlowException;
import com.founder.fix.fixflow.core.impl.cmd.AbstractExpandTaskCmd;
import com.founder.fix.fixflow.core.impl.command.StartProcessInstanceCommand;
import com.founder.fix.fixflow.core.impl.identity.Authentication;
import com.founder.fix.fixflow.core.impl.interceptor.CommandContext;
import com.founder.fix.fixflow.core.impl.persistence.TaskManager;
import com.founder.fix.fixflow.core.impl.task.TaskInstanceEntity;
import com.founder.fix.fixflow.core.runtime.ProcessInstance;
import com.founder.fix.fixflow.core.task.TaskInstance;
import com.founder.fix.fixflow.core.task.TaskQuery;
import com.founder.fix.fixflow.expand.command.SaveTaskDraftCommand;

public class SaveTaskDraftCmd extends AbstractExpandTaskCmd<SaveTaskDraftCommand,Void>  {

	

	protected String formUri;
	


	public SaveTaskDraftCmd(SaveTaskDraftCommand saveTaskDraftCommand) {
		
		super(saveTaskDraftCommand);
		
		
		this.formUri=saveTaskDraftCommand.getFormUri();

		
	}

	

	public Void execute(CommandContext commandContext) {

		if (taskId != null&&!taskId.equals("")) {
			TaskManager taskManager = commandContext.getTaskManager();

			TaskInstance taskInstance = taskManager.findTaskById(taskId);
			
			
			if(taskInstance!=null){
				
				taskInstance.setDraft(true);
				commandContext.getTaskManager().saveTaskInstanceEntity((TaskInstanceEntity)taskInstance);
				
			}
			else{
				throw new FixFlowException("任务 "+taskId+" 不存在！");
			}
		}
		else{
			
			if(processDefinitionKey==null||processDefinitionKey.equals("")){
				throw new FixFlowException("processDefinitionKey 不能为空!");
			}
			
			
			ProcessEngine processEngine =ProcessEngineManagement.getDefaultProcessEngine();
			RuntimeService runtimeService = processEngine.getRuntimeService();
			// runtimeService.getCommandExecutor().setConnection(dbgr.getConnection());

			StartProcessInstanceCommand startProcessInstanceCommand = new StartProcessInstanceCommand();
			startProcessInstanceCommand.setProcessDefinitionKey(processDefinitionKey);
			startProcessInstanceCommand.setBusinessKey(businessKey);
			startProcessInstanceCommand.setStartAuthor(initiator);
			startProcessInstanceCommand.setTransientVariables(transientVariables);
			// startProcessInstanceCommand.setVariables(Variables);
			ProcessInstance processInstanceQueryTo = runtimeService
					.noneStartProcessInstance(startProcessInstanceCommand);

			// 任务第一步提交完还需找到一个待办事宜再执行掉才算真正完成
			String processInstanceId = processInstanceQueryTo.getId();

			TaskService taskService = processEngine.getTaskService();
			TaskQuery taskQuery = taskService.createTaskQuery()
					.processInstanceId(processInstanceId);

			//先去找独占任务没有的话就去找共享任务并完成他
			List<TaskInstance> taskQueryList = taskQuery.taskAssignee(initiator).taskNotEnd()
					.list();

			
			if(taskQueryList.size()>0){
				TaskInstance taskInstance=taskQueryList.get(0);
				taskInstance.setDraft(true);
				taskInstance.setAssignee(Authentication.getAuthenticatedUserId());
				commandContext.getTaskManager().saveTaskInstanceEntity((TaskInstanceEntity)taskInstance);
			}
			else{
				TaskQuery taskQueryNew=taskService.createTaskQuery().processInstanceId(processInstanceId);
				List<TaskInstance> taskQueryCandidateList = taskQueryNew.taskCandidateUser(initiator).taskNotEnd().list();
				if(taskQueryCandidateList.size()>0){
					TaskInstance taskInstanceCandidate=taskQueryCandidateList.get(0);
					taskInstanceCandidate.setDraft(true);
					taskInstanceCandidate.setAssignee(Authentication.getAuthenticatedUserId());
					commandContext.getTaskManager().saveTaskInstanceEntity((TaskInstanceEntity)taskInstanceCandidate);
				}
			}
			
		
			
		
			
			/*
			TaskInstance newTask = TaskInstanceEntity.create();
			newTask.setId(GuidUtil.CreateGuid());
			newTask.setAssignee(Authentication.getAuthenticatedUserId());
			newTask.setCreateTime(new Date());
			newTask.setProcessDefinitionKey(processDefinitionKey);
			ProcessDefinitionManager processDefinitionManager = commandContext.getProcessDefinitionManager();

			ProcessDefinitionBehavior processDefinition = processDefinitionManager.findLatestProcessDefinitionByKey(this.processDefinitionKey);

			UserTask userTask=null;
			Object flowNodeObject = processDefinition.getSubTask();
			if (flowNodeObject != null && flowNodeObject instanceof UserTask) {
				userTask=(UserTask)flowNodeObject;
				newTask.setNodeId(userTask.getId());
				
			}
			
			newTask.setDraft(true);
			
			newTask.setFormUri(formUri);
			
			newTask.setPriority(50);
			
			newTask.setProcessDefinitionId(processDefinition.getProcessDefinitionId());
	
			newTask.setProcessDefinitionName(processDefinition.getName());
			newTask.setTaskInstanceType(TaskInstanceType.FIXFLOWTASK);
			newTask.setBizKey(this.businessKey);
			
			if(newTask==null||newTask.getId()==null||newTask.getId().equals("")){
				throw new FixFlowException("taskId不能为空!");
			}
			
			commandContext.getTaskManager().saveTaskInstanceEntity((TaskInstanceEntity)newTask);*/
		}

	
		return null;


	}

}