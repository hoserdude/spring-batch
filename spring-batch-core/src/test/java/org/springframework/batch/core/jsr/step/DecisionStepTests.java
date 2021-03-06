package org.springframework.batch.core.jsr.step;

import static org.junit.Assert.assertEquals;
import static org.springframework.batch.core.jsr.JsrTestUtils.runJob;

import java.util.List;
import java.util.Properties;

import javax.batch.api.Decider;
import javax.batch.runtime.BatchRuntime;
import javax.batch.runtime.BatchStatus;
import javax.batch.runtime.JobExecution;
import javax.batch.runtime.StepExecution;

import org.junit.Before;
import org.junit.Test;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.beans.factory.access.BeanFactoryLocator;
import org.springframework.beans.factory.access.BeanFactoryReference;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.access.ContextSingletonBeanFactoryLocator;

public class DecisionStepTests {

	private static ApplicationContext baseContext;

	private JobExplorer jobExplorer;

	@Before
	public void setUp() {
		StepExecutionCountingDecider.previousStepCount = 0;

		if(jobExplorer == null) {
			BeanFactoryLocator beanFactoryLocactor = ContextSingletonBeanFactoryLocator.getInstance();
			BeanFactoryReference ref = beanFactoryLocactor.useBeanFactory("baseContext");
			baseContext = (ApplicationContext) ref.getFactory();

			baseContext.getAutowireCapableBeanFactory().autowireBeanProperties(this,
					AutowireCapableBeanFactory.AUTOWIRE_BY_NAME, false);
		}
	}

	public void setJobExplorer(JobExplorer jobExplorer) {
		this.jobExplorer = jobExplorer;
	}

	@Test
	public void testDecisionAsFirstStepOfJob() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionAsFirstStep-context", new Properties(), 10000l);
		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		assertEquals(0, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionThrowsException() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionThrowsException-context", new Properties(), 10000l);
		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		assertEquals(2, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionValidExitStatus() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionValidExitStatus-context", new Properties(), 10000l);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertEquals(3, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionUnmappedExitStatus() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionInvalidExitStatus-context", new Properties(), 10000l);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		List<StepExecution> stepExecutions = BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId());
		assertEquals(2, stepExecutions.size());

		for (StepExecution curExecution : stepExecutions) {
			assertEquals(BatchStatus.COMPLETED, curExecution.getBatchStatus());
		}
	}

	@Test
	public void testDecisionCustomExitStatus() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionCustomExitStatus-context", new Properties(), 10000l);
		assertEquals(BatchStatus.FAILED, execution.getBatchStatus());
		assertEquals(2, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
		assertEquals("CustomFail", execution.getExitStatus());
	}

	@Test
	public void testDecisionAfterFlow() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionAfterFlow-context", new Properties(), 10000l);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertEquals(3, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
	}

	@Test
	public void testDecisionAfterSplit() throws Exception {
		JobExecution execution = runJob("DecisionStepTests-decisionAfterSplit-context", new Properties(), 10000l);
		assertEquals(BatchStatus.COMPLETED, execution.getBatchStatus());
		assertEquals(4, BatchRuntime.getJobOperator().getStepExecutions(execution.getExecutionId()).size());
		assertEquals(2, StepExecutionCountingDecider.previousStepCount);
	}

	public static class StepExecutionCountingDecider implements Decider {

		static int previousStepCount = 0;

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			previousStepCount = executions.length;
			return "next";
		}
	}

	public static class NextDecider implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			for(StepExecution stepExecution : executions) {
				if ("customFailTest".equals(stepExecution.getStepName())) {
					return "CustomFail";
				}
			}

			return "next";
		}
	}

	public static class FailureDecider implements Decider {

		@Override
		public String decide(StepExecution[] executions) throws Exception {
			throw new RuntimeException("Expected");
		}
	}
}
