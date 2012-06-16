/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.autorevert;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;

/**
 * User: dima
 * Date: 10/06/2012
 */
public class StartOrStopAutoRevertAction extends AnAction {
	@Override public void actionPerformed(AnActionEvent event) {
		Project project = event.getProject();
		if (project == null) return;

		AutoRevertProjectComponent autoRevertProjectComponent = project.getComponent(AutoRevertProjectComponent.class);
		if (autoRevertProjectComponent.isStarted()) {
			autoRevertProjectComponent.stop();
		} else {
			autoRevertProjectComponent.start();
		}
	}

	@Override public void update(AnActionEvent event) {
		String text = textFor(event.getProject());
		event.getPresentation().setText(text);
		event.getPresentation().setDescription(text);
		event.getPresentation().setEnabled(event.getProject() != null);
	}

	private static String textFor(Project project) {
		if (project == null) return "Start auto-revert";

		if (project.getComponent(AutoRevertProjectComponent.class).isStarted()) {
			return "Stop auto-revert";
		} else {
			return "Start auto-revert";
		}
	}
}
