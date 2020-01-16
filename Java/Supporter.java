package matisse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.lang3.StringUtils;

import matisse.Run_supporter.supporters_proximity;

/**
 * Supporters are instances of this class, member methods govern their behavior.
 */

public class Supporter {
	/** Currently chosen technology. */
	private Technology chosen_technology;
	/** Name of the currently chosen technology. */
	private String name_of_chosen_technology;
	/** Track technology changes of a supporter. */
	private List<Technology> history_chosen_technologies = new LinkedList<Technology>();
		
	/** Location in the practice space. */
	private double[] practices; 
	/** Direction in which a supporter moves on the practice space. */
	private double[] direction; 
	/** Speed with which supporters change their position on the practice space. */
	private double speed; 
	/** Category, with which a supporter was initialized. */
	private String initial_state;
	/** Current state of the supporter, either rural or urban. */
	private String state;
	/** Influence of the neighborhood on the attractivity of a technology. */
	double neighborhood_influence_factor;
	/** The distance between a supporter and its chosen technology. */
	private double distance;
	/** Coordinates in space of a supporter*/
	private double[] place;
	
	/** The weight of strength determines how much the strength
	 * of a technology influences the supporters decisions on attractivity.
	 * Increasing: More important
	 * Decreasing: Less important
	 */
	private double weight_of_strength = 125;
	
	/** Dissatisfaction for each technology.
	 * Rows: Technologies
	 * Columns: Development of the CDL of each technology
	 */
	private List<List<String>> dissatisfaction = new ArrayList<List<String>>();
	
	/** Random generator, passed along from the builder. */
	Random random_generator;
	
	/**
	 * Constructor, executed every time an object of the class Supporter is created.
	 * 
	 * @param random_generator The random generator initialized in the builder.
	 * @param technologies List of technologies.
	 * @param supporters List of supporters.
	 * @param direction Direction of movement on the practice space.
	 * @param practices Location on the practice space.
	 * @param speed Speed with which supporters change their position on the practice space.
	 * @param place Coordinates on the map of a supporter.
	 * @param initial_state Category, with which a supporter was initialized.
	 */
	public Supporter(Random random_generator, List<Technology> technologies, List<Supporter> supporters, double[] direction, 
			double[] practices, double speed, double[] place, String initial_state,
			double neighborhood_influence_factor) {
		this.random_generator = random_generator;
		this.direction = direction;
		this.practices = practices;
		this.speed = speed;
		this.place = place;
		this.initial_state = initial_state;
		this.state = initial_state;
		this.neighborhood_influence_factor = neighborhood_influence_factor;
	}

	
	/** Supporter picks a technology and
	 * the chosen technology is stored in chosen_technology.
	 * 
	 * @param technologies List of technologies.
	 * @param supporters List of supporters.
	 * @param supporters_proxmity List of supporters to determine neighbors.
	 * @param step Current step.
	 */
	public void choose(List<Technology> technologies, List<Supporter> supporters, List<supporters_proximity> supporters_proximity, int step) {
		// Declare the variable containing the distance between this supporter and another supporter.
		double distance_between_supporters;
		// Declare the variable containing the distance between the supporter and a technology.
		double distance_between_supporter_and_technology = -1;
		// Temporary distance between supporter and a technology, used in the algorithm to search
		// for the most attractive technology.
		double tmp_distance_between_supporter_and_technology;
	
		// Temporarily chosen technology.
		Technology tmp_chosen_technology = null;
		
		// Two indicators to be used in the algorithm to determine the most attractive technology.
		double old_attractivity = Double.NEGATIVE_INFINITY;
		double new_attractivity = Double.NEGATIVE_INFINITY;
		
		// Declare the variable containing the supporters neighbors, who
		// have chosen the same technology as the one whose attractivity
		// it determines.
		int number_of_neighbors_with_same_technology = 0;
		
		
		// Loop through all the supporters in supporters_proximity
		// to calculate the distance between this supporter
		// and all other supporters.
		for(int i = 0; i < supporters_proximity.size(); i++) {	
			// Get the position of the currently selected supporter on the map.
			double[] other_place = supporters_proximity.get(i).get_place();
			
			// Calculate the distance between the supporter and the other supporter
			// by adding the squared difference and taking the square root.
			// place is a member array of supporter objects containing the 
			// position of this supporter on the map.
			distance_between_supporters = (place[0] - other_place[0]) * (place[0] - other_place[0]) +
					(place[1] - other_place[1]) * (place[1] - other_place[1]);
			distance_between_supporters = Math.sqrt(distance_between_supporters);
			
			// Store the distance between this supporter and the other supporter
			// as a member variable in the object representing the other supporter.
			supporters_proximity.get(i).set_distance(distance_between_supporters);
		}
		
		// Sort the list based on the distance between this supporter and the other supporters.
		Collections.sort(supporters_proximity);
		
		
		// Loop through all technologies to determine the most attractive one.
		
		// Randomize the order in which technologies are considered to avoid an advantage of the first
		// technology, in the case of all technologies having the same attractivity.
		// Store the current number of technologies.
		int current_number_of_technologies = technologies.size();
		
		// Declare the array, which contains the order of the technologies.
		int[] random_technologies = new int[current_number_of_technologies];
		
		// Loop through all technologies.
		for(int i = 0; i < random_technologies.length; i++) {
			// Set work to true, to allow the first iteration of the while loop.
			boolean work = true;
			while(work) {
				// Store a random number between 0 and the number of technologies.
				random_technologies[i] = random_generator.nextInt(random_technologies.length);
				// Assume a unique number has been picked and set work to false.
				work = false;
				// Check if there are duplicates by comparing the selected
				// random number and comparing it to all the previously selected numbers.
				for(int k = 0; k < i; k++) {
					if(random_technologies[i] == random_technologies[k] && i != k) {
						work = true;
					}
				}	
			}
		}	
		
		// Use the previously determined order to loop through all technologies.
		for(int i: random_technologies) {
			// True, if the currently selected technology exists.
			if(technologies.get(i) != null) {
				// Set the number of this supporter's neighbors with the same technology to zero.
				number_of_neighbors_with_same_technology = 0;
				// Calculate the distance between the currently selected technology
				// and the supporter.
				tmp_distance_between_supporter_and_technology = calculate_distance(technologies.get(i), step);
				
				// Loop through the 10 next neighbors of this supporter.
				// Skip the first supporter in the list, because that supporter is identical
				// to the supporter examining its neighborhood.
				for(int j = 1; j <= 11; j++) {
					// The order of the list supporters does not change, but the list supporters_proximity changes
					// every time a supporter chooses a technology. To achieve consistency between the two lists
					// the indices in supporters_proximity remain unchanged and correspond to the order of
					// the list supporters.
					if(technologies.get(i) == supporters.get(supporters_proximity.get(j).get_index()).get_chosen_technology()) {
						// Increase the count of the neighbors with the same technology by one
						// number_of_neighbors_with_the_same_technology is defined between [0, 10].
						number_of_neighbors_with_same_technology = number_of_neighbors_with_same_technology + 1;
					}
				}
				
		
				// Calculate the attractivity of the currently selected technology for this supporter.
				new_attractivity = 
						// The strength positively influences the attractivity.
						weight_of_strength * technologies.get(i).get_normalized_strength()
						// Add a summand representing this supporter's neighborhood.
						+ neighborhood_influence_factor * number_of_neighbors_with_same_technology
						// Subtract the dissatisfaction with this technology.
						- get_dissatisfaction(technologies.get(i));
					
				if(Double.isNaN(new_attractivity) == true) {
					System.out.println("ERROR: new_attractivity is not a number.");
				}
						
				// Always true in the first loop iteration.
				// Only true in the following iterations, if
				// the currently selected technology is more attractive.
				if(old_attractivity < new_attractivity) {
					distance_between_supporter_and_technology = tmp_distance_between_supporter_and_technology;
					tmp_chosen_technology = technologies.get(i);
					// If new_attractivity is higher than old_technology, set old_attractivity to new_attractivity.
					// Subsequent iterations only improve the attractivity.
					old_attractivity = new_attractivity;	
				}
			}
		}
		
		// True, if the supporter has chosen a different technology than before.
		// chosen_technology is not equal to tmp_chosen_technology, if a different technology has been chosen in
		// the previous execution of this method.
		// chosen_technology is null, if no technology has been chosen before.
		if(chosen_technology != tmp_chosen_technology && chosen_technology != null) {
			history_chosen_technologies.add(chosen_technology);
		}
		
		// Set the newly chosen technology stored in tmp_chosen_technology
		// as the currently chosen technology in chosen_technology.
		chosen_technology = tmp_chosen_technology;
		// Set the new distance between a supporter and its chosen technology
		// as the new distance.
		distance = distance_between_supporter_and_technology;
		
		if(chosen_technology == null) {
			System.out.println("ERROR: chosen_technology is null.");
		}
		// Store the name of the currently chosen technology.
		name_of_chosen_technology = chosen_technology.get_name();
	}
	
	
	/** Calculate the distance between a supporter and a technology
	 * as Euclidean distance.
	 * 
	 * @param given_technology An instance of the class Technology
	 * @return Distance between a technology and a supporter
	 */
	private double calculate_distance(Technology given_technology, int step) {
		// Declare a variable containing the distance between this supporter
		// and the technology passed along as argument to this method.
		double distance = 0;
		// Declare an array containing the practices of the technology passed
		// along as argument to this method.
		double[] tmp_practices;
		
		// Store the practices of the technology in tmp_practices.
		tmp_practices = given_technology.get_practices();
		
		// Loop through all dimensions of the practice space
		// and sum up the squared differences.
		for(int j = 0; j < practices.length; j++) {
			// Penalize technologies, that are more costly or produce more emissions than desired.
			if(j != 0 && j != 1) {
				distance = distance + 
						(practices[j] - tmp_practices[j]) * (practices[j] - tmp_practices[j]);
			} else if(j == 0) {
					distance = distance +
						(practices[j] - tmp_practices[j]) * (practices[j] - tmp_practices[j]);
			} else if(j == 1) {
					distance = distance +
						(practices[j] - given_technology.get_cost(step)) * (practices[j] - given_technology.get_cost(step));
			}
			
		}
		
		// Take the square root of the summed up squared differences.
		distance = Math.sqrt(distance);
		// Return the distance.
		return distance;
	}
	
	/** Returns the dissatisfaction for a given technology.
	 * 
	 * @param given_technology An object of the class Technology
	 */
	public double get_dissatisfaction(Technology given_technology) {
		// Declare a variable for the dissatisfaction with the given technology.
		String dissatisfaction_with_technology = "";
		
		// Declaration of a variable, which will contain the name of a row on the dissatisfaction matrix.
		String name_dissatisfaction = null;
		// Declaration of a variable, which will contain the name of a technology from the list of technologies.
		String name_technologies = null;
		
		name_technologies = given_technology.get_name();
		
		for(int i = 0; i < dissatisfaction.size(); i++) {
			name_dissatisfaction = dissatisfaction.get(i).get(0);
			if(name_dissatisfaction.equals(name_technologies)) {
				// Check column size.
				int number_of_columns = dissatisfaction.get(i).size();
				dissatisfaction_with_technology = dissatisfaction.get(i).get(number_of_columns - 1);
			}
		}
		
		if(Double.isNaN(Double.parseDouble(dissatisfaction_with_technology)) == true) {
			System.out.println("ERROR: dissatisfaction_with_technology is not a number.");
		}

		return Double.parseDouble(dissatisfaction_with_technology);
	}
	
	/** Updates the dissatisfaction for all technologies.
	 * 
	 * @param technologies The List of technologies
	 * @param step The current step in the simulation
	 */
	public void update_dissatisfaction(List<Technology> technologies, int step) {
		// Declare a variable for the temporary distance between a technology and the supporter.
		double tmp_distance;
		// Declaration of a variable, which will contain the name of a row on the dissatisfaction matrix.
		String name_dissatisfaction = null;
		// Declaration of a variable, which will contain the name of a technology from the list of technologies.
		String name_technologies = null;
		// Declaration of a variable, which is true, if an identity between name_dissatisfaction and name_technologies
		// has been found.
		boolean found_technology = false;
		// The new level of dissatisfaction
		String new_dissatisfaction;
		if(step > 0) {
			// Loop through all technologies to determine the new dissatisfactions with them.
			for(int i = 0; i < technologies.size(); i++) {
				// True, if the technology exist
				if(technologies.get(i) != null) {
					// Assume, the technology will not be found in the following.
					found_technology = false;
					
					// Loop through all rows in dissatisfaction to see if there is 
					// a previous entry for the currently selected technology in dissatisfaction.
					
					for(int j = 0; j < dissatisfaction.size(); j++) {
						// Store the name of the currently selected row of dissatisfaction
						// in name_dissatisfaction.
						name_dissatisfaction = dissatisfaction.get(j).get(0);
						// Store the name of the currently selected technology in
						// name_technologies.
						name_technologies = technologies.get(i).get_name();
						
						// True, if the name of the currently selected row of dissatisfaction equals
						// the name of the currently selected technology.
						if(name_dissatisfaction.equals(name_technologies)) {
							// True, if the currently selected technology is the supporter's chosen technology.
							if(chosen_technology == technologies.get(i)) {
								// Calculate the distance between the supporter and the currently selected technology.
								tmp_distance = calculate_distance(technologies.get(i), step);
								// Calculate the new level of dissatisfaction.
								new_dissatisfaction = String.valueOf(1 * get_dissatisfaction(technologies.get(i)) + 
										1 * tmp_distance);
								// Add the calculated value to the matrix of dissatisfactions.
								dissatisfaction.get(j).add(new_dissatisfaction);
							// The currently selected technology is not the supporter's chosen technology.
							} else {
								boolean chosen_before = false;
								
								// Check, if it has been chosen before.
								for(int k = 0; k < history_chosen_technologies.size(); k++) {
									if(chosen_technology == history_chosen_technologies.get(k)) {
										chosen_before = true;
									}
								}
								
								if(chosen_before == true) {
									tmp_distance = calculate_distance(technologies.get(i), step);
									new_dissatisfaction = String.valueOf(0.75 * tmp_distance + 0.25 * get_dissatisfaction(technologies.get(i)));
									dissatisfaction.get(j).add(new_dissatisfaction);
								} else {
									tmp_distance = calculate_distance(technologies.get(i), step);
									new_dissatisfaction = String.valueOf(0.5 * tmp_distance + 0.5 * get_dissatisfaction(technologies.get(i)));
									dissatisfaction.get(j).add(new_dissatisfaction);
								}
							}
							// Being in this block means, that an identity between an entry on dissatisfaction and
							// the list of technologies has been found, so set this to true.
							found_technology = true;
						}
					}
	
					// If not identity has been found previously, add this technology to the dissatisfaction matrix
					// and compute a new level of dissatisfaction.
					if(found_technology == false) {
						// Add a new row to dissatisfaction.
						dissatisfaction.add(new ArrayList<String>());
						// Get the number of rows
						int number_of_rows = dissatisfaction.size();
						// Add the name of the currently selected technology as the first column of
						// the newly added row.
						dissatisfaction.get(number_of_rows - 1).add(technologies.get(i).get_name());
						// Calculate the distance between this technology and the supporter.
						tmp_distance = calculate_distance(technologies.get(i), step);
						new_dissatisfaction = String.valueOf(tmp_distance);
						dissatisfaction.get(number_of_rows - 1).add(new_dissatisfaction);
					}
				}
			}
		} else if(step == 0) {
			// Loop through all technologies to determine the new dissatisfactions with them.
			for(int i = 0; i < technologies.size(); i++) {
				// True, if the technology exist.
				if(technologies.get(i) != null) {
					// Add a new row to dissatisfaction.
					dissatisfaction.add(new ArrayList<String>());
					// Get the number of rows.
					int number_of_rows = dissatisfaction.size();
					// Add the name of the currently selected technology as the first column of
					// the newly added row.
					dissatisfaction.get(number_of_rows - 1).add(technologies.get(i).get_name());
					// Calculate the distance between this technology and the supporter.
					tmp_distance = calculate_distance(technologies.get(i), step);
					new_dissatisfaction = String.valueOf(tmp_distance);
					dissatisfaction.get(number_of_rows - 1).add(new_dissatisfaction);
				}
			}
		}
	}
	
	
	/** Supporter moves on the practice space depending on
	 * direction and speed and the technology it has chosen. */
	public void move() {
		// Declare a variable, which will contain the new position.
		double new_position;
		
		// Execute, if the chosen technology of this supporter exists.
		if(chosen_technology != null) {
			// Declare an array containing the practices of the chosen technology
			// of this supporter.
			double[] technology_practices = chosen_technology.get_practices();
			// Declare an array containing the direction towards the chosen technology
			// of this supporter.
			double[] direction_towards_technology = new double[direction.length];
			
			// Set the directions towards the chosen technology.
			for(int i = 0; i < direction.length; i++) {
				direction_towards_technology[i] = technology_practices[i] - practices[i];
			}	
			
			// Normalize the directions
			double[] direction_normalized = new double[direction.length];
			double sum = 0;
			for(int i = 0; i < direction.length; i++) {
				sum = sum + direction_towards_technology[i] * direction_towards_technology[i];
			}
			
			for(int i = 0; i < direction.length; i++) {
				direction_normalized[i] = direction_towards_technology[i] / sum;
			}
			
			// Loop through all dimensions of the practice space to set the new practices
			// of this supporter.
			for (int i = 0; i < practices.length; i++) {
				// Exclude the fourth dimension, because it represents BE.
				if(i != 3) {
					// The new position in the currently selected dimension is the current value,
					// plus the move towards the chosen technology,
					// plus the direction the supporter moves times the speed.
					new_position = practices[i] + 0.05 * direction_towards_technology[i] + direction[i] * speed;
					// Contain values too small and too large.
					if (new_position >= 0 && new_position <= 100) {
						practices[i] = new_position;
					}
				}
			}
		}
	}
	
	// Getter and setter for the chosen technology.
	/** Returns the currently chosen technology.
	 * @return chosen_technology. */
	public Technology get_chosen_technology() {
		return chosen_technology;
	}
	/** Set chosen_technology.
	 * @param chosen_technology The new chosen technology. */
	public void set_chosen_technology(Technology chosen_technology) {
		this.chosen_technology = chosen_technology;
	}
	
	// Getter and setter for the direction
	/** Returns the direction.
	 * @return direction The direction of this supporter. */
	public double[] get_direction() {
		return direction;
	}
	/** Add something to the the direction.
	 * @param direction Array containing the summands to the direction. */
	public void add_to_direction(double[] direction) {
		// Loop through all dimensions of the practice space to set
		// the new direction
		for(int i = 0; i < direction.length; i++) {
			// Exclude the fourth dimension, because it represents BE
			if(i != 3) {
				this.direction[i] = this.direction[i] + direction[i];
			}
		}
	}
	
	// Getter and setter for the practices.
	/** Return the practices .
	 * @return practices The practices of this supporter. */
	public double[] get_practices() {
		return practices;
	}
	/** Set the practices.
	 * @param practices Array containing the new practices of this supporter. */
	public void set_practices(double[] practices) {
		this.practices = practices;
	}
	
	// Getter and setter for the speed.
	/** Return speed .
	 * @return speed The speed of this supporter. */
	public double get_speed() {
		return speed;
	}
	/** Set speed.
	 * @param speed The new speed of this supporter. */
	public void set_speed(double speed) {
		this.speed = speed;
	}
	
	// Getter for the place on the map.
	/** Return place.
	 * @return place The place on the map of this supporter. */
	public double[] get_place() {
		return place;
	}
	// Setter for the place on the map.
	public void set_place(double[] place) {
		this.place = place;
	}
	
	// Getter and setter for the state.
	/** Return state.
	 * @return state The state of this supporter. */
	public String get_state() {
		return state;
	}
	/** Set state.
	 * @param state The new state of this supporter .*/
	public void set_state(String state) {
		this.state = state;
	}
	
	// Method to produce data output for each supporter.
	/** Produce data output for each supporter.
	 * @return practices, direction, place, speed, initial_state, state
	 */
	@Override
	public String toString() {
		return name_of_chosen_technology + "," + practices[0] + "," +	practices[1] + "," + practices[2] + "," + 
		practices[3] + "," + practices[4] +	"," + practices[5] + "," +	direction[0] + "," + 
		direction[1] +	"," + direction[2] + "," + direction[3] + "," +	direction[4] + "," + direction[5] +	"," + 
		place[0] + "," + place[1] + "," + speed + "," + initial_state + "," + state + ",";
	}	

}
