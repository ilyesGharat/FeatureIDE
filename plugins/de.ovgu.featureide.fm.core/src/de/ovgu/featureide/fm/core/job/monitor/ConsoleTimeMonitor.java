/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2019  FeatureIDE team, University of Magdeburg, Germany
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
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.core.job.monitor;

import de.ovgu.featureide.fm.core.job.IJob;

/**
 * Control object for {@link IJob}s. Can be used to check for cancel request, display job progress, and calling intermediate functions.
 *
 * @author Sebastian Krieter
 */
public class ConsoleTimeMonitor<T> extends ConsoleMonitor<T> {

	protected long time = System.nanoTime();

	public ConsoleTimeMonitor() {
		super();
	}

	public ConsoleTimeMonitor(boolean output) {
		super(output);
	}

	@Override
	protected void print(String name) {
		if (output) {
			final long oldTime = time;
			time = System.nanoTime();
			System.out.println(name + " -> " + (((time - oldTime) / 1_000_000) / 1_000.0) + "s");
		}
	}

}
