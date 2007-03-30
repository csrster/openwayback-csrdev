/* Driver
 *
 * $Id$
 *
 * Created on 6:08:22 PM Mar 29, 2007.
 *
 * Copyright (C) 2007 Internet Archive.
 *
 * This file is part of wayback.
 *
 * wayback is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 2.1 of the License, or
 * any later version.
 *
 * wayback is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 *
 * You should have received a copy of the GNU Lesser Public License
 * along with wayback-svn; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.archive.wayback.resourceindex.indexer.hadoop;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.ObjectWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.Writable;
import org.apache.hadoop.io.WritableComparable;
import org.apache.hadoop.mapred.ClusterStatus;
import org.apache.hadoop.mapred.JobClient;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.MapReduceBase;
import org.apache.hadoop.mapred.Mapper;
import org.apache.hadoop.mapred.OutputCollector;
import org.apache.hadoop.mapred.Reporter;
import org.apache.hadoop.mapred.TextOutputFormat;
import org.apache.hadoop.mapred.lib.IdentityReducer;
import org.apache.hadoop.util.ReflectionUtils;
import org.archive.io.arc.ARCRecord;
import org.archive.mapred.ARCMapRunner;
import org.archive.wayback.resourceindex.indexer.ArcIndexer;

/**
 * Hadoop Driver for generation of alphabettically partitioned Wayback CDX 
 * files using the Hadoop framework. 
 *
 * @author brad
 * @version $Date$, $Revision$
 */
public class Driver {
	

	/**
	 * Mapper which converts an ARCRecord into a CDX line.
	 *
	 * @author brad
	 * @version $Date$, $Revision$
	 */
	public static class MapClass extends MapReduceBase implements Mapper {

		private Text outKey = new Text();
		private Text outValue = new Text("");
		public void map(WritableComparable key, Writable value,
				OutputCollector output, Reporter reporter) throws IOException {
			ObjectWritable ow = (ObjectWritable) value;
			ARCRecord rec = (ARCRecord) ow.get();
			String line;
			try {
				line = ArcIndexer.arcRecordToCDXLine(rec);

				outKey.set(line);
				output.collect(outKey, outValue);
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
	}

	static void printUsage() {
		System.out.println("[-m <maps>] <input> <output>");
		System.exit(1);
	}

	/**
	 * The main driver for sort program.
	 * Invoke this method to submit the map/reduce job.
	 * @param args 
	 * @throws IOException When there is communication problems with the 
	 *                     job tracker.
	 */
	public static void main(String[] args) throws IOException {
		Configuration defaults = new Configuration();

		JobConf jobConf = new JobConf(defaults, Driver.class);
		jobConf.setJobName("cdx1");

		jobConf.setMapRunnerClass(ARCMapRunner.class);
		
//		jobConf.setInputFormat(SequenceFileInputFormat.class);
		jobConf.setOutputFormat(TextOutputFormat.class);

		jobConf.setOutputKeyClass(Text.class);
		jobConf.setOutputValueClass(Text.class);
		jobConf.set("mapred.partitioner.class",
				"org.apache.hadoop.examples.AlphaPartitioner");

		jobConf.setMapperClass(MapClass.class);        
//		jobConf.setMapperClass(IdentityMapper.class);
		jobConf.setReducerClass(IdentityReducer.class);

		AlphaPartitioner part = (AlphaPartitioner)ReflectionUtils.newInstance(
				jobConf.getPartitionerClass(), jobConf);
		int num_reduces = part.getNumPartitions();
		
		
		JobClient client = new JobClient(jobConf);
		ClusterStatus cluster = client.getClusterStatus();
		int num_maps = cluster.getTaskTrackers()
				* jobConf.getInt("test.sort.maps_per_host", 10);
		List <String>otherArgs = new ArrayList<String>();
		for (int i = 0; i < args.length; ++i) {
			try {
				if ("-m".equals(args[i])) {
					num_maps = Integer.parseInt(args[++i]);
				} else {
					otherArgs.add(args[i]);
				}
			} catch (NumberFormatException except) {
				System.out.println("ERROR: Integer expected instead of "
						+ args[i]);
				printUsage();
			} catch (ArrayIndexOutOfBoundsException except) {
				System.out.println("ERROR: Required parameter missing from "
						+ args[i - 1]);
				printUsage(); // exits
			}
		}

		jobConf.setNumMapTasks(num_maps);
		jobConf.setNumReduceTasks(num_reduces);

		// Make sure there are exactly 2 parameters left.
		if (otherArgs.size() != 2) {
			System.out.println("ERROR: Wrong number of parameters: "
					+ otherArgs.size() + " instead of 2.");
			printUsage();
		}
		jobConf.setInputPath(new Path((String) otherArgs.get(0)));
		jobConf.setOutputPath(new Path((String) otherArgs.get(1)));

		// Uncomment to run locally in a single process
		//job_conf.set("mapred.job.tracker", "local");

		System.out.println("Running on " + cluster.getTaskTrackers()
				+ " nodes to sort from " + jobConf.getInputPaths()[0]
				+ " into " + jobConf.getOutputPath() + " with " + num_reduces
				+ " reduces.");
		Date startTime = new Date();
		System.out.println("Job started: " + startTime);
		JobClient.runJob(jobConf);
		Date end_time = new Date();
		System.out.println("Job ended: " + end_time);
		System.out.println("The job took "
				+ (end_time.getTime() - startTime.getTime()) / 1000
				+ " seconds.");
	}

}