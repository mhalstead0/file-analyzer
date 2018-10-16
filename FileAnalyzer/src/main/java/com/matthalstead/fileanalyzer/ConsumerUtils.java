package com.matthalstead.fileanalyzer;

import java.util.ArrayList;
import java.util.List;

public class ConsumerUtils {

	public static ByteSink buildAndStartSinkForConsumers(InputStreamConsumer ... consumers) {
		List<ByteSink> sinks = new ArrayList<ByteSink>(consumers.length);
		for (int i=0; i<consumers.length; i++) {
			ByteSink sink = new SinkToConsumerRunner(consumers[i]);
			sinks.add(sink);
		}
		return new ByteSinkCombiner(sinks);
	}
}
