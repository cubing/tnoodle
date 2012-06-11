package net.gnehzr.tnoodle.scrambles;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.List;
import java.util.SortedMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.gnehzr.tnoodle.utils.BadClassDescriptionException;
import net.gnehzr.tnoodle.utils.LazyInstantiator;
import net.gnehzr.tnoodle.utils.TNoodleLogging;
import net.gnehzr.tnoodle.utils.TimedLogRecordStart;

public class Main {
	private static final Logger l = Logger.getLogger(Main.class.getName());
	
	public static void main(String[] args) throws IllegalArgumentException, SecurityException, InstantiationException, IllegalAccessException, InvocationTargetException, ClassNotFoundException, NoSuchMethodException, BadClassDescriptionException, IOException {

		OptionParser parser = new OptionParser();
		OptionSpec<?> benchmark = parser.acceptsAll(Arrays.asList("b", "benchmark"), "Benchmark scramble generation");
		OptionSet options = parser.parse(args);
		List<String> puzzles = options.nonOptionArguments();

		if(options.has(benchmark)) {
			TNoodleLogging.initializeLogging(Level.ALL);
		} else {
			TNoodleLogging.initializeLogging();
		}
		
		SortedMap<String, LazyInstantiator<Scrambler>> scramblers = Scrambler.getScramblers();
		for(String puzzle : puzzles) {
			LazyInstantiator<Scrambler> lazyScrambler = scramblers.get(puzzle);
			if(lazyScrambler == null) {
				System.err.println("couldn't find puzzle " + puzzle + ", try one of " + scramblers.keySet());
				System.exit(1);
			}
			
			TimedLogRecordStart start = new TimedLogRecordStart("Generating " + puzzle + " scramble");
			l.log(start);
			
			Scrambler s = lazyScrambler.cachedInstance();
			System.out.println(s.generateScramble());
			
			l.log(start.finishedNow());
		}
	}
}
