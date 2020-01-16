package matisse;

// Import java libraries.
import java.util.List;
import java.util.Random;
// Import a Apache Commons Math library to compute the Pearsons Correlation coefficient.
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/** Technologies are instances of this class, member methods govern their behavior */

public class Technology {
	// Basic properties.
	/** Name of the technology. */
	private String name; 
	/** State of the technology: Regime, ENA or niche. */
	private String state_of_technology; 
	/** Percentage of supporters, between 0 and 1. */
	private double number_of_supporters;
	/** number_of_supporters of the previos step, between 0 and 1. */
	private double number_of_supporters_old;
	
	// Practices, directions, frontiers.
	/** Practices of this technology. */
	private double[] practices; 
	/** Direction on the practice space of this technology. */
	private double[] direction; 
	/** Limits in which a technology can move, lower and upper bound. */
	private double [][] frontier; 
	
	// Strength.
	/** Strength in this step. */
	private double strength;
	/** Strength in the last step. */
	private double strength_old;
	/** Normalized strength, between 0 and 1. */
	double normalized_strength;
	
	// Metabolism.
	/** Resources generated in this step. */
	private double resources;
	/** Fraction of the resources spent on movement in the practice space. */
	double fraction_movement = 0.5;
	/** Fraction of the resources spent on amassing strength. */
	double fraction_strength = 0.5;
	
	// General adaption parameters.
	/** Adaption rate of a technology. */
	private final double adaption_rate = 3;
	/** Maximum distance between two technologies with
	 * which clustering or absorption can take place. */
	private final double max_distance_absorption = 100; 
	
	// Adaption parameters of the regime.
	/** Speed of adaption of a regime. */
	private final double adaption_speed_regime = 1; 
	/** Probability with which the regime absorbs a niche. */
	private final double p_absorption = 0.3; 
	/** Maximum angle between the directions of two technologies
	 * with which clustering or absorption can take place. */
	private final double max_angle_absorption = 0.5 * Math.PI; 
	
	/** The average values of supporters of the regime. */
	double[] own_supporters_average_previous;
	/** The name of the current tactical contender. */
	String current_tactical_contenders_name = "null";
	/** The step in which the current tactical contender was identified. */
	int current_tactical_contender_step;
	/** The name of the previous tactical contender. */
	String previous_tactical_contenders_name = "null";
	/** The step in which the previous tactical contender was identified. */
	int previous_tactical_contender_step;

	// Adaption parameters of a niche.
	/** Speed of adaption of a niche. */
	private final double adaption_speed_niche = 1.5; 
	/** Probability with which two niches cluster. */
	private final double p_clustering = 0.3; 
	/** Technologies cannot be absorbed with a normalized strength above this value. */
	private final double level_absorption = 0.75; 
	
	// Subsidies for a technology.
	/** Point in time, when the subsidy starts. */
	int start_of_subsidy;
	/** Point in time, where the subsidy ends. */
	int end_of_subsidy;
	/** Size of the subsidy, in [0, 1] */
	double subsidy_percentage;
	
	/** Random generator, passed along from the builder */
	Random random_generator;
	
	/**
	 * Constructor of a technology.
	 * 		
	 * @param random_generator Random generator.
	 * @param name Name of the technology.
	 * @param direction Direction of the technology in the practice space.
	 * @param practices Location in the practice space.
	 * @param frontier Frontiers in the practice space.
	 * @param strength Initial strength.
	 */
	public Technology(Random random_generator, String name, double[] direction, 
			double[] practices, double[][] frontier, double strength) {
		// Set the parameters passed along from the builder to the member variables of this object.
		this.random_generator = random_generator;
		this.name = name;
		this.strength = strength;
		this.direction = direction;
		this.practices = practices;
		this.frontier = frontier;
		
		// Determine, whether a technology is the regime, an ENA or a niche.
		transformation();
	
		// Check consistency of practices and frontiers by looping through all practices
		// and comparing them to the frontiers.
		for(int i = 0; i < practices.length; i++) {
			if(practices[i] < frontier[i][0]) {
				System.out.println("NOTICE: For technology " + this.get_name() + " the practice " + i+1 + " is lower than the lower frontier.");
			}
			if(practices[i] > frontier[i][1]) {
				System.out.println("NOTICE: For technology " + this.get_name() + " the practice " + i+1 + " is higher than the upper frontier.");
			}
		}	
	}
	
	/**
	 * The regime tries to maximize support over its own supporters as long as its strength increases.
	 * When its strengths decreases, it tries to identify the niche or the ENA, which is a threat to 
	 * its own position.
	 * 
	 * @param supporters List of all supporters.
	 * @param technologies List of all technologies.
	 * @param summary_output The history of the strength development of all technologies.
	 * @param step Current step in the simulation.
	 */
	
	public void adaption_regime (List<Supporter> supporters, List<Technology> technologies, List<List<String>> summary_output, int step) {
		// An array for the practices of a supporter.
		double[] practices_of_supporter = new double[practices.length];
		// An array for the average values of the supporters of the regime.
		double[] own_supporters_average = new double[practices.length];
		// A tactical contender is identifiable short-term and it can become a strategic contender.
		Technology tactical_contender = null; 
		// A strategic contenders strength is inversely related with the own strength.
		Technology strategic_contender = null;
		
		// Arrays, which will contain the strength development.
		double[] history_own_strength = new double[4];
		double[] history_other_strength = new double[4];
		// Variables indicating, whether the collected data is consistent.
		boolean valid_own_data = true;
		boolean valid_contender_data = true;
		
		// The correlation between strengths.
		double tmp_correlation_between_strengths;
		double correlation_between_strengths = Double.POSITIVE_INFINITY;
		// The name of the strategic contender.
		String strategic_contenders_name = "null";
		// Variable indicating, if a strategic contender has been found.
		boolean strategic_contender_detected = false;
	
		/*
		 * Calculate the average values of the own supporters and set direction towards it.
		 */
		// True, if the strength increased.
		if(strength > strength_old) {		
			// Loop through the own supporters to get the sums of the values.
			for(int i = 0; i < supporters.size(); i++) {
				// True, if it is an own supporter.
				if(supporters.get(i).get_chosen_technology() == this) {
					// Store the practices of the currently selected supporter
					// in practices_of_supporter.
					practices_of_supporter = supporters.get(i).get_practices();
					
					// Loop through all dimensions and sum up the individual practices.
					for(int j = 0; j < own_supporters_average.length; j++) {
						own_supporters_average[j] = own_supporters_average[j] + practices_of_supporter[j];
					}
				}
			}
			// Divide by the number of supporters to get the average values.
			for(int i = 0; i < own_supporters_average.length; i++) {
				own_supporters_average[i] = own_supporters_average[i] / (this.number_of_supporters * supporters.size());
			}
			// Set the direction towards those of the average own supporter.
			for(int i = 0; i < direction.length; i++) {
				direction[i] = own_supporters_average[i] - practices[i];
			}
		/*
		 * Try to identify a tactical and strategic contender and set the direction towards it.
		 */
		// True, if the strength decreased or remained constant.
		} else if(strength <= strength_old) {
			// Observe all relative strength increases. If one technology has the most
			// relative strength increase in two subsequent simulation steps, 
			// it is a tactical contender.
			
			// If the name of the current tactical contender is not null, a potential tactical contender
			// has been detected in the previous step.
			if(current_tactical_contenders_name != "null") {
				// Move the name and step, in which a potential tactical contender
				// has been found, back.
				previous_tactical_contenders_name = current_tactical_contenders_name;
				previous_tactical_contender_step = current_tactical_contender_step;
			}
		
			// Assume no tactical contender will be found, which can be falsified in the
			// following block.
			boolean tactical_contender_detected = false;
			
			// Use summary_output to detect the current tactical contender
			// Loop through all rows of summary_output
			for(int i = 0; i < summary_output.size(); i++) {
				// and through all columns to detect the biggest relative increase.
				for(int j = 0; j < summary_output.get(i).size(); j++) {
					// The current strength of a technology.
					double current_strength;
					// The previous strength of a technology.
					double previous_strength;
					// The relative strength increase between the previous and the current step.
					double relative_strength_increase;
					// Algorithmically detect the biggest increase and store it in this variable.
					double maximum_relative_strength_increase = Double.NEGATIVE_INFINITY;
					
					// There have to be at least two values.
					if(summary_output.get(i).size() >= 3) {
						// True, if the values are neither "null" nor "0.0" and the currently selected
						// technology is not identical to the active technology.
						if(summary_output.get(i).get(summary_output.get(i).size() - 1) != "null" &&
								summary_output.get(i).get(summary_output.get(i).size() - 1) != "0.0" &&
								summary_output.get(i).get(summary_output.get(i).size() - 2) != "null" &&
								summary_output.get(i).get(summary_output.get(i).size() - 1) != "0.0" &&
								summary_output.get(i).get(0) != this.get_name()) {
		 					// Get the current strength from the last row entry.
							current_strength = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 1));
							// Get the previous strength from the second last row entry.
							previous_strength = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 2));
							
							// Calculate the relative strength increase.
							relative_strength_increase = current_strength / previous_strength;
							
							// True, if the relative strength increase is greater than that of the technology
							// previously selected.
							if(relative_strength_increase > maximum_relative_strength_increase) {
								// Set the current relative strength increase as the maximum strength increase.
								maximum_relative_strength_increase = relative_strength_increase;
								// Store the name of the potential tactical contender.
								current_tactical_contenders_name = summary_output.get(i).get(0);
								// Store the step, in which this contender has been identified.
								current_tactical_contender_step = step;
							}
						}
					}
				}
			}
			
			// True, if the candidates match
			if(current_tactical_contenders_name == previous_tactical_contenders_name &&
					// and if they have been found in two subsequent steps.
					current_tactical_contender_step - 1 == previous_tactical_contender_step) {
				// Contender identified.
				tactical_contender_detected = true;
				
				// Loop through the technologies to identify the technology
				for(int i = 0; i < technologies.size(); i++) {
					if(technologies.get(i) != null &&
							technologies.get(i).get_name().toLowerCase().contains(current_tactical_contenders_name.toLowerCase())) {
						tactical_contender = technologies.get(i);
					}
				}
			}
		
			// Detect strategic contender by the correlation between the own strength decrease and contender's strength increase.
		    // Loop through the development of the strength of all technologies to
			// store the own strength in history_own_strength.
			for(int i = 0; i < summary_output.size(); i++) {
				// True if the row is that of development of the own strength
				if(summary_output.get(i).get(0) == this.get_name() &&
					// and if there are at least four values
					summary_output.get(i).size() >= 5) {
					// Check for nulls in the data
					for(int j = 1; j < 5; j++) {
						if(summary_output.get(i).get(summary_output.get(i).size() - j) == "null") {
							// If a "null" has been found, the data is invalid.
							valid_own_data = false;
						}
					}
					// True, if the data on the development of the own strength is valid.
					if(valid_own_data == true) {
						// Store the data in the array. It has to be cast to Double, because it is stored as String.
						history_own_strength[3] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 1));
						history_own_strength[2] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 2));
						history_own_strength[1] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 3));
						history_own_strength[0] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 4));
					}		
				}
			}
			
			// Get the strength development of all other technologies, calculate a correlation coefficient and select
			// the highest one, by looping through the data of all technologies.
			for(int i = 0; i < summary_output.size(); i++) {
				// True if the row is not that of the regime
				if(summary_output.get(i).get(0) != this.get_name() &&
					// and if there are at least four values.
					summary_output.get(i).size() >= 5) {
					// Assume the data of the possible contender is valid.
					valid_contender_data = true;
					// Check for nulls in the data.
					for(int j = 1; j < 5; j++) {
						if(summary_output.get(i).get(summary_output.get(i).size() - j) == "null") {
							// The data is invalid.
							valid_contender_data = false;
						}
					}
					// True if both the own and the contender's data are valid.
					if(valid_own_data == true && valid_contender_data == true) {
						// Store the data in the array.
						history_other_strength[3] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 1));
						history_other_strength[2] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 2));
						history_other_strength[1] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 3));
						history_other_strength[0] = Double.parseDouble(summary_output.get(i).get(summary_output.get(i).size() - 4));
						
						// Calculate the correlation coefficient.
						tmp_correlation_between_strengths = new PearsonsCorrelation().correlation(history_own_strength, history_other_strength);
						
						// True, if the return variable is a number
						if(Double.isNaN(tmp_correlation_between_strengths) == false &&
								// and the value between the regime and the currently selected technology is greater than that of the previous one
								tmp_correlation_between_strengths < correlation_between_strengths &&
								// and they are inversely correlated
								tmp_correlation_between_strengths < 0) {
							// Set the newly detected maximum as maximum.
							correlation_between_strengths = tmp_correlation_between_strengths;
							// Store the name of the technology.
							strategic_contenders_name = summary_output.get(i).get(0);
							// Set the boolean variable to true.
							strategic_contender_detected = true;
						}
					}
				}
			}
			
			// Loop through all technologies to identify the strategic contender.
			for(int i = 0; i < technologies.size(); i++) {
				// True, if the currently selected technology exists
				if(technologies.get(i) != null && 
						// and the name has been set
						strategic_contenders_name != "null" &&
						// and the name, which was determined previously, is identical to the name of the technology.
						technologies.get(i).get_name() == strategic_contenders_name) {
					// Set the strategic contender.
					strategic_contender = technologies.get(i);
				}
			}
			
			// True, if a strategic and tactical contender have been set.
			if(strategic_contender != null && tactical_contender != null &&
					tactical_contender_detected == true && strategic_contender_detected == true &&
					// AND if they are identical
					strategic_contender == tactical_contender) {
				// Declare a variable for the contenders practices
				double[] contenders_practices = new double[practices.length];
				// Set the direction of the regime towards the contender
				for(int i = 0; i < direction.length; i++) {
					direction[i] = contenders_practices[i] - practices[i];
				}	
			}
		}
		
		// Execute the movement.
		
		// Declare a variable for the normalized direction.
		double[] direction_normalized = new double[direction.length];
		// Declare a variable for the sum of each dimension.
		double sum = 0;
		
		// Create the sums for each dimension.
		for(int i = 0; i < direction.length; i++) {
			// Using the squared sum, since the direction can be negative.
			sum = sum + direction[i] * direction[i];
		}
		
		// Divide each direction by the sum.
		for(int i = 0; i < direction.length; i++) {
			direction_normalized[i] = direction[i] / sum;
		}		

		// Loop through all the dimensions of the practice space to set the new direction.
		for (int i = 0; i < practices.length; i++) {
			double new_practice;
			// True, if the dimension is not Cost.
			if(i != 1) {
				// The dimensions are emissions, MIT, PT, BE and Conv.
				new_practice = practices[i] +
						(direction_normalized[i] * fraction_movement * resources * adaption_rate * adaption_speed_regime);
				// Keep the new practices within the frontiers.
				if(new_practice > frontier[i][0] && new_practice < frontier[i][1]) {
					practices[i] = new_practice;
				}
			} else if(i == 1) {
				// Cost development.
				new_practice = practices[i] +
						(direction_normalized[i] * fraction_movement * resources * adaption_rate * adaption_speed_regime);
				calculate_cost(new_practice, step);
			}
		}
	}	
	
	/** An ENA moves closer to the regime, if it exists, otherwise it acts like a niche.
	 * 
	 * @param supporters List of all supporters.
	 * @param technologies List of all technologies.
	 * @param step Current step in the simulation.
	 */
	
	public void adaption_ena (List<Supporter> supporters, List<Technology> technologies, int step) {
		// Declare an array for the practices of the regime.
		double[] regime_practices = new double[practices.length];
		// Declare a variable for the new value in a dimension of the practice space.
		double new_practice;
		
		// True, if a regime exists and then act as "predator".
		if(regime_exists(technologies) == true) {			
			// Get the practices of the regime.
			for(int i = 0; i < technologies.size(); i++) {
				if(technologies.get(i) != null) {
					if(technologies.get(i).get_state_of_technology() == "regime") {
						regime_practices = technologies.get(i).get_practices();
					}
				}
			}
			// Set the directions towards the regime.
			for(int i = 0; i < direction.length; i++) {
				direction[i] = regime_practices[i] - practices[i];
			}	
			
			// Execute the movement.
			
			// Declare an array for the normalized direction.
			double[] direction_normalized = new double[direction.length];
			// Declare a variable for the sum of each dimension.
			double sum = 0;
			
			// Create the sums for each dimension.
			for(int i = 0; i < direction.length; i++) {
				// Using the squared sum, since the direction can be negative.
				sum = sum + direction[i] * direction[i];
			}
			
			// Divide each direction by the sum .
			for(int i = 0; i < direction.length; i++) {
				direction_normalized[i] = direction[i] / sum;
			}
			
			// Loop through all the dimensions of the practice space to set the new direction.
			for (int i = 0; i < practices.length; i++) {
				// True, if the dimension is not Cost.
				if(i != 1) {
					// The dimensions are emissions, MIT, PT, BE and Conv.
					new_practice = practices[i] +
							(direction_normalized[i] * fraction_movement * resources * adaption_rate * adaption_speed_niche);
					// Keep the new practices within the frontiers.
					if (new_practice > frontier[i][0] && new_practice < frontier[i][1]) {
						practices[i] = new_practice;
					}
				} else if(i == 1) {
					// Cost development.
					new_practice = practices[i] +
							(direction_normalized[i] * fraction_movement * resources * adaption_rate * adaption_speed_niche);
					calculate_cost(new_practice, step);
				}
				
			}
		} else {
			// Act as a "hunter".
			adaption_niche(supporters, technologies, step);
		}
	}
	
	/** Niches move randomly on the practices space, if their strength
	 * has fallen or not increased. They keep the direction of their movement,
	 * if their strength increases.
	 * 
	 * @param technologies List of technologies.
	 * @param step Current time step..
	 */
	public void adaption_niche (List<Supporter> supporters, List<Technology> technologies, int step) {
		// True, if the number of supporters has fallen or remained constant.
		if(strength <= strength_old) {

			// Loop through all the practice space directions.
			for(int i = 0; i < direction.length; i++) {
				// Randomly assign the direction values
				// between -1 and 1.
				if(i != 0) {
					direction[i] = (random_generator.nextDouble() - 0.5) * 2;
				} else {
					direction[i] = random_generator.nextDouble();
				}
			}			
		}
		
		// Execute the movement.
		
		// Declare a variable for the normalized direction.
		double[] direction_normalized = new double[direction.length];
		// Declare a variable for the sum of each dimension.
		double sum = 0;
		
		// Create the sums for each dimension.
		for(int i = 0; i < direction.length; i++) {
			sum = sum + direction[i] * direction[i];
		}
		
		// Divide each direction by the sum.
		for(int i = 0; i < direction.length; i++) {
			direction_normalized[i] = direction[i] / sum;
		}
		
		// Loop through all the dimensions of the practice space to set the new direction.
		for (int i = 0; i < practices.length; i++) {
			double new_practice;
			// True, if the dimension is not Cost.
			if(i != 1) {
				// The dimensions are emissions, MIT, PT, BE and Conv.
				if(fraction_movement * resources > 0.05) {
					new_practice = practices[i] +
							(direction_normalized[i] * fraction_movement * resources * adaption_rate * adaption_speed_niche);
				} else {
					new_practice = practices[i] +
							(direction_normalized[i] * 0.05 * adaption_rate * adaption_speed_niche);				
				}
				
				// Keep the new practices within the frontiers.
				if (new_practice > frontier[i][0] && new_practice < frontier[i][1]) {
					practices[i] = new_practice;
				}
			} else {
				new_practice = practices[i] +
						(direction_normalized[i] * fraction_movement * resources * adaption_rate * adaption_speed_niche);
				calculate_cost(new_practice, step);
			}
		}
	}
	
	
	/**
	 * Absorption by proximity. A regime can cluster with a niche, if they are in
	 * proximity of each other and move in similar directions.
	 * 
	 * @param technologies List of technologies
	 * @return index of the absorbed technology or -1
	 * 			
	 */
	public int absorption_proximity(List<Technology> technologies){
		double tmp_distance = 0;
		double sum_of_scalar_products_1 = 0;
		double sum_of_scalar_products_2 = 0;
		double sum_of_scalar_products_3 = 0;
		double distance = 0;
		double angle = 0;

		// True, if the normalized strength is below the threshold.
		if(normalized_strength < level_absorption) { 
			// Loop through all technologies.
			for(int i = 0; i < technologies.size(); i++) {
				// True, if
				// 1.) the selected technology exists,
				// 2.) is not identical to the acting technology,
				// 3.) is not the regime.
				if(technologies.get(i) != null && 
						technologies.get(i).name != this.name && 
						technologies.get(i).state_of_technology == "niche") {
					tmp_distance = 0;
					
					// Calculate the distance.
					for(int j = 0; j < practices.length; j++) {
						tmp_distance = tmp_distance + (practices[j] - technologies.get(i).practices[j]) * 
							(practices[j] - technologies.get(i).practices[j]);
					}
					
					distance = Math.sqrt(tmp_distance);
				
					// Calculate the angle.
					sum_of_scalar_products_1 = 0;
					sum_of_scalar_products_2 = 0;
					sum_of_scalar_products_3 = 0;
					
					for(int j = 0; j < direction.length; j++) {
						sum_of_scalar_products_1 = sum_of_scalar_products_1 + direction[j] * technologies.get(i).direction[j];
						sum_of_scalar_products_2 = sum_of_scalar_products_2 + direction[j] * direction[j];
						sum_of_scalar_products_3 = sum_of_scalar_products_3 + technologies.get(i).direction[j] *
							technologies.get(i).direction[j];				
					}
					
					angle = Math.acos(sum_of_scalar_products_1 / (Math.sqrt(sum_of_scalar_products_2) * Math.sqrt(sum_of_scalar_products_3)));	
				
					if(distance <= max_distance_absorption && 
							angle <= max_angle_absorption) {
						if(p_absorption * (1 - 1.2 * (distance / 100)) >= random_generator.nextDouble()) {
							for(int j = 0; j < practices.length; j++) {
								// Set the new practice as weighted average of both practices.
								practices[j] = (practices[j] * normalized_strength +
									technologies.get(i).practices[j] *
									technologies.get(i).normalized_strength) /
									(normalized_strength +
										technologies.get(i).normalized_strength);
								// Set the new direction as weighted average of both directions.
								direction[j] = (direction[j] * normalized_strength+
									technologies.get(i).direction[j] *
									technologies.get(i).normalized_strength) /
									(normalized_strength +
										technologies.get(i).normalized_strength);
								
								// The absorbing technology lowers its lower bound, if
								// the absorbed technology has a lower frontier.
								if(frontier[j][0] > technologies.get(i).frontier[j][0]) {
									frontier[j][0] = technologies.get(i).frontier[j][0];
								}
								
								// The absorbing technology raises its upper bound, if
								// the absorbed technology has a higher frontier.
								if(frontier[j][1] < technologies.get(i).frontier[j][1]) {
									frontier[j][1] = technologies.get(i).frontier[j][1];
								}
							}
							// Combine the strengths and discount 20%, which are adjustment costs.
							set_strength((this.get_strength() + technologies.get(i).get_strength()) * 0.8);
							
							// Return the index of the absorbed technology.
							return i;
						}
					}
				}
			}	
		}
		// Absorption has failed, return -1.
		return -1;
	}	
	
	/** Two niches can cluster, if they are in proximity of each other
	 *  on the practice space and if they move in a similar direction.
	 *  The resulting technology is a combination of the clustering and
	 *  the clustered technology.
	 *  
	 * @param technologies List of all technologies.
	 * @return Index of the technology, with which the clustering technology clustered. Or -1.
	 */
	public int clustering(List<Technology> technologies) {
		double squared_distance = 0;
		double sum_of_scalar_products_1;
		double sum_of_scalar_products_2;
		double sum_of_scalar_products_3;
		double distance = 0;
		double angle = 0;
		
		// Loop through all technologies
		for(int i = 0; i < technologies.size(); i++) {
			// Condition is true, if
			// 1.) the currently selected technology exist,
			// 2.) the currently selected technology is not identical to the technology that tries to cluster,
			// 3.) the currently selected technology is a niche.
			if(technologies.get(i) != null && 
					technologies.get(i).name != this.name && 
					technologies.get(i).state_of_technology == "niche" ) {
				// Reset squared distance.
				squared_distance = 0;

				/* Calculate the distance. */
				
				// Loop through all technologies.
				for(int j = 0; j < practices.length; j++) {
					squared_distance = squared_distance + (practices[j] - technologies.get(i).practices[j]) * 
						(practices[j] - technologies.get(i).practices[j]);
				}
				distance = Math.sqrt(squared_distance);
				
				/* Calculate the angle. */
				
				sum_of_scalar_products_1 = 0;
				sum_of_scalar_products_2 = 0;
				sum_of_scalar_products_3 = 0;
				
				// Loop through all practices of the technology.
				// Calculation: https://de.wikipedia.org/wiki/Standardskalarprodukt#Winkel
				for(int j = 0; j < direction.length; j++) {
					sum_of_scalar_products_1 = sum_of_scalar_products_1 + direction[j] * technologies.get(i).direction[j];
					sum_of_scalar_products_2 = sum_of_scalar_products_2 + direction[j] * direction[j];
					sum_of_scalar_products_3 = sum_of_scalar_products_3 +
						technologies.get(i).direction[j] * technologies.get(i).direction[j];				
				}
				angle = Math.acos(sum_of_scalar_products_1 / (Math.sqrt(sum_of_scalar_products_2) * Math.sqrt(sum_of_scalar_products_3)));
				
				/* Adjust the values of both technologies */
				
				// True, if calculated distance and angle are both smaller than cutoff values.
				if(distance <= max_distance_absorption && angle <= max_angle_absorption) {
					if(p_clustering * (1 - (distance/100)) >= random_generator.nextDouble()) {
						// Loop through all dimensions of the practices.
						for(int j = 0; j < practices.length; j++) {
							// New practice is the arithmetic mean of both technologies.
							practices[j] =
								(practices[j] + technologies.get(i).practices[j]) / 2;
							// New direction is the arithmetic mean of both directions.
							direction[j]=
								(direction[j] + technologies.get(i).direction[j]) / 2;
							
							// Clustering technology lowers the lower bound of the frontier
							// of the clustered technology.
							if(frontier[j][0] > technologies.get(i).frontier[j][0]) {
								frontier[j][0] = technologies.get(i).frontier[j][0];
							}
							
							// Clustering technology raises the upper bound of the frontier
							// of the clustered technology.
							if(frontier[j][1] < technologies.get(i).frontier[j][1]) {
								frontier[j][1] = technologies.get(i).frontier[j][1];
							}
						}
						// Combine the strengths.
						set_strength(0.90 * (this.get_strength() + technologies.get(i).get_strength()));
						// Clustering has been successful, return the index
						// of the technology, which was clustered with.
						return i;
					}
				}
			}
		}
		// No clustering possible.
		return -1;	
	}

	/** Calculates number_of_supporters for one technology
	 *  as percentage of all supporters.
	 * 
	 * @param supporters List of supporters
	 */
	public void calculate_number_of_supporters(List<Supporter> supporters){
		double count = 0;
		// Loop through all supporters.
		for(int i = 0; i < supporters.size(); i++) {
			if(this == supporters.get(i).get_chosen_technology()) {
				count = count + 1;
			}
		}
		number_of_supporters_old = number_of_supporters;
		
		number_of_supporters = count / supporters.size();
	}	

	/** Calculate the strength of a technology. */
	public void calculate_strength() {
		double const_institutional_capacity = 1;
		double depreciation = 0.1;
		
		strength = strength_old * (1 - depreciation) + const_institutional_capacity * resources * fraction_strength;
	}
	
	/** Calculate the resources generated in this step. */
	public void calculcate_resources() {
		double const_support = 100;
		resources = number_of_supporters * const_support;
	}	

	
	/** Adapt the cost.
	 * 
	 * @param new_cost New cost, which the technology wants to achieve.
	 * @param step Current step in the simulation.
	 */
	public void calculate_cost(double new_cost, int step) {
		// True, if the strength increased or remained constant.
		if(strength >= strength_old) {
			// Decrease cost.
			// The closer a technology gets to its lower frontier, the harder is it to
			// lower the cost.
			practices[1] = practices[1] - ((practices[1] - frontier[1][0]) * 0.01 * random_generator.nextDouble());
		// True, if the strength decreased.
		} else if(strength < strength_old) {
			if(step > 1) {
				// If the technology wants to increase its costs, raise them by 0.1%.
				if(new_cost > practices[1]) {
					//practices[1] = practices[1] * 1.01;
					practices[1] = new_cost;
				}
				// If the technology wants to decrease its costs, also raise them by 0.1%.
				if(new_cost < practices[1]) {
					practices[1] = practices[1] * 1.01;
				}
			}
		}
		

		// Keep the practice within its frontiers.
		if(practices[1] < frontier[1][0]) {
			practices[1] = frontier[1][0];
		}
		
		if(practices[1] > frontier[1][1]) {
			practices[1] = frontier[1][1];
		}
		// Store the current strength as old strength for the next simulation step.
		strength_old = strength;
	}
	
	/** Tells, if a regime exists or not.
	 * 
	 * @param technologies List of all technologies.
	 * @return true or false.
	 */
	public boolean regime_exists(List<Technology> technologies) {
		for(int i = 0; i < technologies.size(); i++) {
			if(technologies.get(i) != null) {
				if(technologies.get(i).get_state_of_technology() == "regime") {
					return true;
				}
			}
		}
		return false;
	}
	

	/** Set the state of a technology depending on its strength */
	public void transformation() {
		if(normalized_strength > 0.4) {	
			state_of_technology = "regime";
		} else if(normalized_strength > 0.1) {
			state_of_technology = "ena";
		} else {
			state_of_technology = "niche";
		}
	}
	
	/** Return the current cost of this technology. */
	public double get_cost(int step) {
		// Check if subsidy is active.
		if(step >= start_of_subsidy && step <= end_of_subsidy) {
			// Calculate the subsidized cost.
			double cost = this.practices[1] - (this.practices[1] * subsidy_percentage);
			// Check if the subsidized cost is within the frontiers.
			if(cost < frontier[1][0]) {
				// Return the lower frontier, if the subsidizes cost is below the frontier.
				return frontier[1][0];
			} else if(cost > frontier[1][0]) {
				// Return the upper frontier, if the subsidizes cost is above the frontier.
				return frontier[1][1];
			} else {
				// Return the subsidized cost.
				return cost;
			}
		// No subsidy is active
		} else {
			return practices[1];
		}
	}
	
	/** Set the parameters of a subsidy. */
	public void set_subsidy(int subsidy_duration, double subsidy_percentage, int step) {
		this.start_of_subsidy = step;
		this.end_of_subsidy = step + subsidy_duration;
		this.subsidy_percentage = subsidy_percentage;
	}

	/** Getter for strength. */
	public double get_strength() {
		return strength;
	}
	/** Setter for strength. */
	public void set_strength(double new_strength) {
		strength = new_strength;
	}
	/** Getter for number_of_supporters */
	public double get_number_of_supporters() {
		return number_of_supporters;
	}
	/** Setter for number_of_supporters. */
	public void set_number_of_supporters(double number_of_supporters) {
		this.number_of_supporters = number_of_supporters;
	}
	/** Getter for practices. */
	public double[] get_practices() {
		return practices;
	}
	/** Setter for practices. */
	public void set_practices(double[] practices) {
		this.practices = practices;
	}
	/** Getter for name. */
	public String get_name() {
		return name;
	}
	/** Setter for name. */
	public void set_name(String new_name) {
		name = new_name;
	}
	/** Getter for state_of_technology. */
	public String get_state_of_technology() {
		return state_of_technology;
	}
	/** Getter for direction. */
	public double[] get_direction() {
		return direction;
	}
	/** Getter for normalized_strength. */
	public double get_normalized_strength() {
		return normalized_strength;
	}
	/** Setter for normalized_strength. */
	public void set_normalized_strength(double sum_strength) {
		normalized_strength = strength / sum_strength;
	}

	@Override
	public String toString() {
		return  name + "," + state_of_technology + "," + number_of_supporters + ","+ strength + "," +
 				practices[0] + "," + practices[1] + "," + practices[2] + "," + practices[3] + "," + practices[4] + "," + practices[5] + "," +
				direction[0] + "," + direction[1] + "," + direction[2] + "," + direction[3] + "," +	direction[4] + "," + direction[5] + ",";
		
	}

}
