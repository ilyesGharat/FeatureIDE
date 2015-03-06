/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package de.ovgu.featureide.ui.statistics.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;

import de.ovgu.featureide.fm.core.PropertyConstants;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.FeatureModelEditor;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.ui.UIPlugin;
import de.ovgu.featureide.ui.statistics.core.ContentProvider;
import de.ovgu.featureide.ui.statistics.ui.helper.JobDoneListener;
import de.ovgu.featureide.ui.statistics.ui.helper.TreeClickListener;
import de.ovgu.featureide.ui.statistics.ui.helper.TreeLabelProvider;

/**
 * View to calculate and show the statistics of a feature project.
 * 
 * @author Dominik Hamann
 * @author Patrick Haese
 */
public class FeatureStatisticsView extends ViewPart implements GUIDefaults {
	private TreeViewer viewer;
	private ContentProvider contentProvider;
	private IWorkbenchPart currentEditor;
	

	
	
	public static final String ID = UIPlugin.PLUGIN_ID + ".statistics.ui.FeatureStatisticsView";
	
	public static final Image EXPORT_IMG = FMUIPlugin.getImage("export_wiz.gif");
	public static final Image REFRESH_IMG = FMUIPlugin.getImage("refresh_tab.gif");

	@Override
	public void createPartControl(Composite parent) {
		

		viewer = new TreeViewer(parent);
		contentProvider = new ContentProvider(viewer);
		viewer.setContentProvider(contentProvider);
		viewer.setLabelProvider(new TreeLabelProvider());
		viewer.setInput(viewer);
		viewer.addDoubleClickListener(new TreeClickListener(viewer));
		ColumnViewerToolTipSupport.enableFor(viewer);
		
		getSite().getPage().addPartListener(editorListener);
		IWorkbenchPage page = getSite().getPage();
		setEditor(page.getActiveEditor());
		
		
		addButtons();
	}

	/**
	 * 
	 */
	private void addButtons() {
		
		Action checkBoxer = new Action() {
			public void run() {
				CheckBoxTreeViewDialog dial = new CheckBoxTreeViewDialog(viewer.getControl().getShell(), contentProvider.godfather, viewer);
				dial.open();
			}
		};
		
		IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
		toolBarManager.add(checkBoxer);
		checkBoxer.setImageDescriptor(ImageDescriptor.createFromImage(EXPORT_IMG));
		checkBoxer.setToolTipText("Export to *.csv");
		
		//-----------------
		Action refresher = new Action() {
			public void run() {
				FeatureStatisticsView.this.refresh();
			}
		};
		
		toolBarManager.add(refresher);
		refresher.setImageDescriptor(ImageDescriptor.createFromImage(REFRESH_IMG));
		refresher.setToolTipText("Refresh View");
		//---------------
	}
	
	private IPartListener editorListener = new IPartListener() {
		
		public void partOpened(IWorkbenchPart part) {}
		
		public void partDeactivated(IWorkbenchPart part) {}
		
		public void partClosed(IWorkbenchPart part) {
			if (part == currentEditor) {
				setEditor(null);
			}
		}
		
		public void partBroughtToTop(IWorkbenchPart part) {
			if (part instanceof IEditorPart)
				setEditor(part);
		}
		
		public void partActivated(IWorkbenchPart part) {
			if (part instanceof IEditorPart) {
				ResourceUtil.getResource(((IEditorPart) part).getEditorInput());
				setEditor(part);
			}
		}
	};
	
	@Override
	public void setFocus() {
		viewer.getControl().setFocus();
	}
	
	/**
	 * Listener that refreshes the view every time the model has been edited.
	 */
	private PropertyChangeListener modelListener = new PropertyChangeListener() {
		public void propertyChange(PropertyChangeEvent evt) {
			if (!PropertyConstants.MODEL_LAYOUT_CHANGED.equals(evt.getPropertyName()))
				refresh();
		}
		
	};
	
	private Job job = null;
	
	/**
	 * Refresh the view.
	 */
	private void refresh() {
		System.out.println("Ich werde aufgerufen!!!! :) ");
		if (contentProvider.isCanceled()) {
			System.out.println("contentProvider is canceled");
			return;
		}
		
		/*
		 * This job waits for the calculation job to finish and starts
		 * immediately a new one
		 */
		Job waiter = new Job("Updating FeatureStatisticsView") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				try {
					System.out.println("run getryed");
					if (job != null) {
						if (contentProvider.isCanceled()) {
							return Status.OK_STATUS;
						}
						contentProvider.setCanceled(true);
						job.join();
						contentProvider.setCanceled(false);
					}
				} catch (InterruptedException e) {
					FMUIPlugin.getDefault().logError(e);
				}
				
				
				job = new Job("Updating FeatureStatisticsView") {
					@Override
					protected IStatus run(IProgressMonitor monitor) {
						if (currentEditor == null) {
							contentProvider.defaultContent();
							System.out.println("contentProvider default");
						} else {
							IResource anyFile = ResourceUtil.getResource(((IEditorPart) currentEditor).getEditorInput());
							//TODO is refresh really necessary? -> true?
							contentProvider.calculateContent(anyFile, true);
							System.out.println("contentProvider calculate");
						}
						return Status.OK_STATUS;
					}
				};
				job.setPriority(Job.DECORATE);
				job.schedule();
				return Status.OK_STATUS;
			}
		};
		waiter.setPriority(Job.DECORATE);
		waiter.schedule();
		cancelJobs();
		System.out.println("Ich bin fresh");
	}
	
	private void cancelJobs() {
		JobDoneListener jobListener = JobDoneListener.getInstance();
		if (jobListener != null) {
			jobListener.cancelAllRunningTreeJobs();
		}
	}
	
	public TreeViewer getViewer() {
		return viewer;
	}
	
	/**
	 * Watches changes in the feature model if the selected editor is an
	 * instance of @{link FeatureModelEditor}
	 */
	private void setEditor(IWorkbenchPart activeEditor) {
		if (currentEditor != null) {
			if (currentEditor == activeEditor) {
				return;
			}
			
			if (currentEditor instanceof FeatureModelEditor) {
				((FeatureModelEditor) currentEditor).getFeatureModel().removeListener(modelListener);
			}
		}
		
		currentEditor = activeEditor;
		if (activeEditor instanceof FeatureModelEditor) {
			((FeatureModelEditor) currentEditor).getFeatureModel().addListener(modelListener);
		}
		refresh();
	}
}
