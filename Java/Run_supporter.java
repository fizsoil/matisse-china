package matisse;

// Import Java libraries.
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
// Import Repast Simphony libraries.
import repast.simphony.engine.schedule.ScheduledMethod;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.random.RandomHelper;
// Import the library to print a diagram with the history
// of strengths of the technologies in single runs.
import org.jfree.ui.RefineryUtilities;
// Import libraries to get the current date. Used
// in batch runs to provide additional information
// in the output.
import java.time.format.DateTimeFormatter;  
import java.time.LocalDateTime;    

/*
 * This class is the central part of the model.
 */
public class Run_supporter {	
	/*
	 * Variable declarations for general parameters
	 */
	private String name;
	static int simulation_steps;
	public static boolean logging;
	public static boolean movement;
	public static boolean landscape_signals;
	public static int landscape_scenario;
	public static boolean batch_run;

	/* 
	 * Variable declarations for all technologies
	 */
	private double[] technology_ice_practices, technology_ice_directions;
	private double[][] technology_ice_frontiers;
	private double technology_ice_strength;

	private double[] technology_fcv_practices, technology_fcv_directions;
	private double[][] technology_fcv_frontiers;
	private double technology_fcv_strength;

	private double[] technology_bev_practices, technology_bev_directions;
	private double[][] technology_bev_frontiers;
	private double technology_bev_strength;
	
	private double[] technology_publictransport_practices, technology_publictransport_directions;
	private double[][] technology_publictransport_frontiers;
	private double technology_publictransport_strength;

	private double[] technology_slowmodes_practices, technology_slowmodes_directions;
	private double[][] technology_slowmodes_frontiers;
	private double technology_slowmodes_strength;
		
	/*
	 * Variable declarations for all supporters
	 */
	/** The total number of supporter */
	private int number_of_supporters;
	
	/** The initial urbanization rate */
	private double initial_urbanization_rate;
	
	/** Initial practices of urban supporters */
	private double[] supporters_urban_practices;
	/** Initial directions of the practices of urban supporters */
	private double[] supporters_urban_directions;
	
	/** Initial practices of rural supporters */
	private double[] supporters_rural_practices;
	/** Initial directions of the practices of rural supporters */
	private double[] supporters_rural_directions;
	
	/** Count of absorption or clustering events */
	int absorbtion_clustering_count = 0;
	
	/*
	 * List declarations for the two acting groups
	 */
	/** The list of all technologies. Contains objects of the class Technologies */
	private static List<Technology> technologies = new LinkedList<Technology>();
	/** The list of all supporters. Contains objects of the class Supporter */
	private static List<Supporter> supporters = new LinkedList<Supporter>(); 
	
	/*
	 * List declarations to store and print the results of the simulation.
	 */
	/** Contains the strength of each technology for each step.
	 * Rows: Technologies
	 * Columns: Steps */	
	public static List<List<String>> summary_output = new ArrayList<List<String>>();
	
	/** Contains all data of each technology for each step.
	 * Rows: Technologies
	 * Columns: Steps */
	private static List<List<String>> technologies_output = new ArrayList<List<String>>();
	
	/** Contains all data of each supporter for each step.
	 * Rows: Supporters
	 * Columns: Steps */
	private static List<List<String>> supporters_output = new ArrayList<List<String>>();
	
	/*
	 * List declarations for urbanization and the "neighborhood effect".
	 */
	// This list is used to implement urbanization.
	/** Contains objects of the class supporters_urbanization */
	private List<supporters_urbanization> supporters_urbanization_list = new ArrayList<supporters_urbanization>();
	// This list is used to sort the supporters by distance from one supporter, which
	// is needed to calculate the "neighborhood effect" in the attractivity function of a supporter.
	/** Contains objects of the lcass supporters_proximity */
	private List<supporters_proximity> supporters_proximity_list = new ArrayList<supporters_proximity>();
	
	/*
	 * Variable declarations for the spatial distribution and urbanization.
	 */
	/** Contains the coordinates of the three cities */
	private double[][] coordinates_of_cities = {{0, 0}, {0, 0}, {0, 0}};
	/** The factor to scale all BE values to be in [0, 100]. */
	double be_scale_factor = 0;
	/** The maximum value of BE determined initially. */
	private double max_be = 0;
	/** The maximum value of BE determined initially, but scaled, so that
	 * all BE values are in [0, 100]. */
	double max_be_scaled = 0;
	/** Threshold value of BE to divide rural from urban supporters.
	 * Below: Rural
	 * Equal or above: Urban */
	private double frontier_urban_rural = 0;
	
	// Variable declarations for the population density map, which is used to determine the values
	// of BE for each supporter.
	/** Two dimensional and quadratic, so the squared edge length equals the resolution. */
	double population_density_resolution = 100;
	/** Number of horizontal cells. */
	int number_of_x_cells = (int)(Math.sqrt(population_density_resolution));
	/** Number of vertical cells. */
	int number_of_y_cells = (int)(Math.sqrt(population_density_resolution));
	/** The density map ranges from [0, 9] in both dimensions. */
	int[][] density_map = new int[number_of_x_cells][number_of_y_cells];
	
	/*
	 * Randomization
	 */
	/** Random number generator, seed is set in the GUI of the simulation */
	Random random_generator = new Random(RandomHelper.getSeed());
	
	/** The current step in the simulation. */
	int step;
	
	/*
	 * Setters for general parameters.
	 */
	public void set_name(String name) {
		this.name = name;
	}
	public void set_simulation_steps(int simulation_steps) {
		Run_supporter.simulation_steps = simulation_steps;
	}
	public void set_logging(boolean logging) {
		Run_supporter.logging = logging;
	}
	public void set_movement(boolean movement) {
		Run_supporter.movement = movement; 
	}
	public void set_landscape_signals(boolean landscape_signals) {
		Run_supporter.landscape_signals = landscape_signals;
	}
	public void set_landscape_scenario(int landscape_scenario) {
		Run_supporter.landscape_scenario = landscape_scenario;
	}
	public void set_batch_run(boolean batch_run) {
		Run_supporter.batch_run = batch_run;
	}

	/*
	 * Setters for all the parameters of the technologies
	 */
	public void set_technology_ice_parameters(double[] technology_ice_practices, double[] technology_ice_directions, 
			double[][] technology_ice_frontiers, double technology_ice_strength) {
		this.technology_ice_practices = technology_ice_practices;
		this.technology_ice_directions = technology_ice_directions;
		this.technology_ice_frontiers = technology_ice_frontiers;
		this.technology_ice_strength = technology_ice_strength;
	}
	public void set_technology_fcv_parameters(double[] technology_fcv_practices, double[] technology_fcv_directions, 
			double[][] technology_fcv_frontiers, double technology_fcv_strength) {
		this.technology_fcv_practices = technology_fcv_practices;
		this.technology_fcv_directions = technology_fcv_directions;
		this.technology_fcv_frontiers = technology_fcv_frontiers;
		this.technology_fcv_strength = technology_fcv_strength;
	}
	public void set_technology_bev_parameters(double[] technology_bev_practices, double[] technology_bev_directions, 
			double[][] technology_bev_frontiers, double technology_bev_strength) {
		this.technology_bev_practices = technology_bev_practices;
		this.technology_bev_directions = technology_bev_directions;
		this.technology_bev_frontiers = technology_bev_frontiers;
		this.technology_bev_strength = technology_bev_strength;
	}
	public void set_technology_publictransport_parameters(double[] technology_publictransport_practices, double[] technology_publictransport_directions, 
			double[][] technology_publictransport_frontiers, double technology_publictransport_strength) {
		this.technology_publictransport_practices = technology_publictransport_practices;
		this.technology_publictransport_directions = technology_publictransport_directions;
		this.technology_publictransport_frontiers = technology_publictransport_frontiers;
		this.technology_publictransport_strength = technology_publictransport_strength;
	}
	
	public void set_technology_slowmodes_parameters(double[] technology_slowmodes_practices, double[] technology_slowmodes_directions, 
			double[][] technology_slowmodes_frontiers, double technology_slowmodes_strength) {
		this.technology_slowmodes_practices = technology_slowmodes_practices;
		this.technology_slowmodes_directions = technology_slowmodes_directions;
		this.technology_slowmodes_frontiers = technology_slowmodes_frontiers;
		this.technology_slowmodes_strength = technology_slowmodes_strength;
	}

	/*
	 * Setters for the parameters of the supporters
	 */
	public void set_number_supporters(int number_of_supporters) {
		this.number_of_supporters = number_of_supporters;
	}
	public void set_initial_urbanization_rate(double initial_urbanization_rate) {
		this.initial_urbanization_rate = initial_urbanization_rate;
	}
		
	public void set_supporters_urban_practices(double[] supporters_urban_practices) {
		this.supporters_urban_practices = supporters_urban_practices;
	}
	public void set_supporters_urban_directions(double[] supporters_urban_directions) {
		this.supporters_urban_directions = supporters_urban_directions;
	}

	public void set_supporters_rural_practices(double[] supporters_rural_practices) {
		this.supporters_rural_practices = supporters_rural_practices;
	}
	public void set_supporters_rural_directions(double[] supporters_rural_directions) {
		this.supporters_rural_directions = supporters_rural_directions;
	}
	
	/*
	 * Class to sort supporters by distance to each other.
	 */
	public class supporters_proximity implements Comparable <supporters_proximity> {
		/** The index corresponds with the order of the list supporters, so that
		concordance between both lists is maintained. **/
		private Integer index;
		/** The position of this supporter on the map. */
		private double[] place;
		/** The distance between this supporter and another one. */
		private Double distance;
		
		/* 
		 * Getters and setters for the index, the place and the distance.
		 */
		void set_index(int index) {
			this.index = index;
		}
		int get_index() {
			return index;
		}
		
		void set_place(double[] place) {
			this.place = place;
		}
		double[] get_place() {
			return place;
		}
		  
		Double get_distance() {
			return distance;
		}
		void set_distance(double distance) {
			this.distance = distance;
		}
		
		// compareTo uses the distance to order the objects of the class supporters_proximity.
		@Override
		public int compareTo(supporters_proximity o) {
			return this.get_distance().compareTo(o.get_distance());
		}
	}
	
	/*
	 * Class to sort supporters by their values of BE.
	 */
	public class supporters_urbanization implements Comparable <supporters_urbanization> {
		  private Integer index;
		  private double[] place;
		  private Double distance;
		  private Double be;
		  private Integer city;
		  
		  void set_index(int index) {
			  this.index = index;
		  }
		  int get_index() {
			  return index;
		  }
		  
		  void set_place(double[] place) {
			  this.place = place;
		  }
		  double[] get_place() {
			  return place;
		  }
		  
		  Double get_distance() {
			  return distance;
		  }
		  void set_distance(double distance) {
			  this.distance = distance;
		  }
		  
		  void set_be(double be) {
			  this.be = be;
		  }
		  Double get_be() {
			  return be;
		  }
		  
		  void set_city(int city) {
			  this.city = city;
		  }
		  int get_city() {
			  return this.city;
		  }
		  
		  @Override
		  public int compareTo(supporters_urbanization o) {
			  return this.get_be().compareTo(o.get_be());
		  }
	}

	/**
	 * Initialize all technologies and supporters and prepare the output.
	 */
	public void initialisation(){
		// Clear all the lists, in case they have content from previous runs.
		summary_output.clear();
		technologies_output.clear();
		supporters_output.clear();	
		
		supporters.clear();
		technologies.clear();
		
		supporters_proximity_list.clear();
		supporters_urbanization_list.clear();
		
		// Initialize the technologies.
		initalize_technologies();
		// Initialize the supporters.
		initialize_supporters();
		
		// Sustainability index.
		sustainability_index();
	
		// Initialize output arrays.
		// Store the initial technology names.
		for(int i = 0; i < technologies.size(); i++) {
			summary_output.add(new ArrayList<String>());
			summary_output.get(i).add(technologies.get(i).get_name());
			summary_output.get(i).add(Double.toString(technologies.get(i).get_strength()));
						
			technologies_output.add(new ArrayList<String>());
			technologies_output.get(i).add(technologies.get(i).get_name());
			technologies_output.get(i).add(technologies.get(i).toString());
		}
		
		// Initialize output array.
		for(int i = 0; i < supporters.size(); i++) {
			supporters_output.add(new ArrayList<String>());
			supporters_output.get(i).add(supporters.get(i).toString());
		}
	}
	
	/**
	 * Initializes all technologies and adds them to the list technologies.
	 */
	private void initalize_technologies() {
		// Toggle movement off by setting the directions to zero.
		if(movement == false) {
			for(int i = 0; i < technology_ice_directions.length; i++) {
				technology_ice_directions[i] = 0;
				technology_fcv_directions[i] = 0;
				technology_bev_directions[i] = 0;
				technology_publictransport_directions[i] = 0;
				technology_slowmodes_directions[i] = 0;
			}
		}

		// Initialize ICE (Regime)
		Technology ice = new Technology(random_generator, "ICE",
				technology_ice_directions, technology_ice_practices, technology_ice_frontiers, 
				technology_ice_strength);
		
		// Initialize FCV
		Technology fcv = new Technology(random_generator, "FCV", 
				technology_fcv_directions, technology_fcv_practices, technology_fcv_frontiers, 
				technology_fcv_strength);	
		
		// Initialize bev
		Technology bev = new Technology(random_generator, "BEV", 
				technology_bev_directions, technology_bev_practices, technology_bev_frontiers, 
				technology_bev_strength);	
		
		// Initialize publictransport
		Technology publictransport = new Technology(random_generator, "Publictransport", 
				technology_publictransport_directions, technology_publictransport_practices, technology_publictransport_frontiers, 
				technology_publictransport_strength);	
		
		// Initialize slowmodes
		Technology slowmodes = new Technology(random_generator, "Slowmodes", 
				technology_slowmodes_directions, technology_slowmodes_practices, technology_slowmodes_frontiers, 
				technology_slowmodes_strength);	
				
		// Add the initialized technologies to the list technology
		technologies.add(ice);	
		technologies.add(fcv);
		technologies.add(bev);
		technologies.add(publictransport);
		technologies.add(slowmodes);		
	}
	
	/**
	 *	Initialize all supporters and add them to the list supporters.
	 */
	private void initialize_supporters() {
		// Declare the array practices with the length of the practices passed along from
		// the Builder.
		double[] practices = new double[supporters_urban_practices.length];
		
		// Declare the array directions with the length of the practices passed along from
		// the Builde.r
		double[] directions = new double[supporters_urban_directions.length];
	
		// Declare the variable speed, which governs how fast the practices of one supporter
		// change.
		double speed;
		
		// Toggle movement off, if the parameter movement is false.
		if(movement == false) {
			// Loop through all dimensions.
			for(int i = 0; i < supporters_urban_directions.length; i++) {
				// Set the directions to zero.
				supporters_urban_directions[i] = 0;
				supporters_rural_directions[i] = 0;
			}
		}
		
		/*
		 *  Randomly distribute the supporters on the map.
		 */
		
		// Randomly designate three cities.
		// Loop through all cities.
		for(int i = 0; i < coordinates_of_cities.length; i++) {
			// Loop through the coordinates of each city.
			for(int j = 0; j < 2; j++) {
				// Repeat the random assignment until three distinct pairs are chosen 
				// within the borders of the map. 
				// The coordinates follow a discrete random distribution: U(0, 99)
				do {
					coordinates_of_cities[i][j] = random_generator.nextInt(100);
				} while(coordinates_of_cities[i][j] < 0 || coordinates_of_cities[i][j] > 100);
			}
		}
		
		// Position the supporters on the map by looping through all supporters.
		for(int i = 0; i < number_of_supporters; i++) {
			// Declare the array containing the position of the supporter.
			double[] own_position = {0, 0};
			// Randomly select a city.
			int chosen_city = random_generator.nextInt(coordinates_of_cities.length); 
			// Declare variables for the distance between the supporter and the next city.
			double distance_from_city;
			double tmp_distance_from_city;
			
			// Chose the coordinates of the supporter on the map.
			do {
				// Set the distance to negative infinity, so the first iteration selects the first
				// city as the closest one and subsequent iterations can be compared to the first
				// iteration.
				tmp_distance_from_city = Double.NEGATIVE_INFINITY;
				
				// The distance between the supporter and his city follows a normal distribution:
				// distance_from city ~ N(0, 20)
				distance_from_city = random_generator.nextGaussian() * 20;
				// Contain negative values
				if(distance_from_city <= 0) {
					distance_from_city = distance_from_city * (-1);
				}
				
				// Randomly select an angle
				double angle = random_generator.nextDouble() * Math.PI * 2;
				// Chose the abscissa
				own_position[0] = coordinates_of_cities[chosen_city][0] + Math.cos(angle) * distance_from_city;
				// Chose the ordinate
				own_position[1] = coordinates_of_cities[chosen_city][1] + Math.sin(angle) * distance_from_city;
				
				// Due to the normal distribution of the distance, the supporter can be closer
				// to another city than the one originally selected.
				// Confirm, that the originally selected city is the closest or change its city
				// by comparing all three distances.
				for(int j = 0; j < coordinates_of_cities.length; j++) {
					// Calculate the distance between the position of the supporter and the city.
					tmp_distance_from_city = (own_position[0] - coordinates_of_cities[j][0]) *
							(own_position[0] - coordinates_of_cities[j][0]) +
							(own_position[1] - coordinates_of_cities[j][1]) *
							(own_position[1] - coordinates_of_cities[j][1]);
					tmp_distance_from_city = Math.sqrt(tmp_distance_from_city);
					
					// Compare the calculated distance to the original distance.
					if(tmp_distance_from_city < distance_from_city) {
						distance_from_city = tmp_distance_from_city;
						// Set the closest city as new city.
						chosen_city = j;
					}
				}
			// Repeat, if the position is out of the limits of the map
			} while(own_position[0] < 0 || own_position[0] > 100 ||
					own_position[1] < 0 || own_position[1] > 100);
			
			// Create new_supporter as an instance of the class supporters_urbanization.
			supporters_urbanization new_supporter = new supporters_urbanization();
			// Set the index to maintain concordance with other lists of supporters.
			//new_supporter.set_index(i);
			// Set the coordinates
			new_supporter.set_place(own_position);
			// Set the distance
			new_supporter.set_distance(distance_from_city);
			// Set the closest city
			new_supporter.set_city(chosen_city);
			// Add the supporter to the list of supporters;
			supporters_urbanization_list.add(new_supporter);
		}
		
		// Create the density map.
		create_density_map();
		// Determine the value of BE for each supporter.
		determine_be();
		
		// The maximum value of BE is that of the first supporter on the list.
		max_be = supporters_urbanization_list.get(0).get_be();
				
		// Index of the last urban supporter.
		int last_urban_supporter = (int)(supporters_urbanization_list.size() * initial_urbanization_rate);
		// The frontier between urban and rural supporters is the value of BE of the last urban supporter.
		frontier_urban_rural = supporters_urbanization_list.get(last_urban_supporter).get_be();
		
		// Scale BE to 0-100.
		// Only execute, if the biggest value of BE is > 100.
		if(supporters_urbanization_list.get(0).get_be() > 100) {
			// Get the scale factor.
			be_scale_factor = 100 / supporters_urbanization_list.get(0).get_be();
			// Declare a variable for the scaled BE value.
			double scaled_be;
			
			// Loop through all supporters to apply the scaling.
			for(int i = 0; i < supporters_urbanization_list.size(); i++) {
				// Apply the scale factor.
				scaled_be = be_scale_factor * supporters_urbanization_list.get(i).get_be();
				// Save the scaled value of BE.
				supporters_urbanization_list.get(i).set_be(scaled_be);
			}
		}
		
		// The maximum value of BE is that of the first supporter on the list.
		max_be_scaled = supporters_urbanization_list.get(0).get_be();
		
		/*
		 * Initialize the urban population.
		 */
		Supporter urban_supporter;
		for(int i = 0; i < last_urban_supporter; i++) {
			// Set the index to maintain concordance with other lists of supporters.
			supporters_urbanization_list.get(i).set_index(i);
			
			practices = new double[supporters_urban_practices.length];
			// Set the values of all six dimension of the practice space.
			// The values of the dimensions follow N(supporters_rural_practice[i], 20).
			// Emissions
			practices[0] = supporters_urban_practices[0] + (random_generator.nextGaussian() * 20);
			// Cost
			practices[1] = supporters_urban_practices[1] + (random_generator.nextGaussian() * 20);
			// MIT
			practices[2] = supporters_urban_practices[2] + (random_generator.nextGaussian() * 20);
			// BE
			practices[3] = supporters_urbanization_list.get(i).get_be();
			// PT
			practices[4] = supporters_urban_practices[4] + (random_generator.nextGaussian() * 20);
			// Conv
			practices[5] = supporters_urban_practices[5] + (random_generator.nextGaussian() * 20);
			
			// Catch outliers.
			for(int j = 0; j < practices.length; j++) {
				// Do not apply the transformation to the value of BE, because it is already in range.
				if(j != 3) {
					if (practices[j] < 0) practices[j] = 0;
					if (practices[j] > 100) practices[j] = 100;
				}
			}

			// The speed of a supporter is distributed according to N(6, 3).
			speed = random_generator.nextGaussian() * 3 + 6; 
			// Set the value of speed to zero if it is below 1.
			if(speed < 1) {
				speed = 0;
			}
			
			// Set the directions of the values of the six dimensions of the practice space.
			directions = new double[practices.length];
			
			// Loop through all dimensions.
			for(int j = 0; j < supporters_urban_directions.length; j++) {
				directions[j] = supporters_urban_directions[j];
			}
			
			// Get the place from the previously defined list.
			double[] place = supporters_urbanization_list.get(i).get_place();
			
			// Set the initial state to "urban".
			String initial_state = "urban";
			
			// Set how strong the decision on the attractivity of the technologies
			// is determined by the supporters neighborhood.
			double neighborhood_influence_factor = 1;
			
			// Create urban_supporter as an instance of the class Supporter.
			urban_supporter =
				new Supporter(random_generator, technologies, supporters, directions, practices, speed, 
						place, initial_state, neighborhood_influence_factor);
			// Add the supporter to the list of supporters.
			supporters.add(urban_supporter);
		}
		
		/*
		 * Initialize the rural population.
		 */
		Supporter rural_supporter;
		for(int i = last_urban_supporter; i < supporters_urbanization_list.size(); i++) {
			// Set the index to maintain concordance with other lists of supporters.
			supporters_urbanization_list.get(i).set_index(i);
			
			practices = new double[supporters_rural_practices.length];
			// Set the values of all six dimension of the practice space.
			// The values of the dimensions follow N(supporters_rural_practice[i], 20).
			// Emissions
			practices[0] = supporters_rural_practices[0] + (random_generator.nextGaussian() * 20);
			// Cost
			practices[1] = supporters_rural_practices[1] + (random_generator.nextGaussian() * 20);
			// MIT
			practices[2] = supporters_rural_practices[2] + (random_generator.nextGaussian() * 20);
			// BE
			practices[3] = supporters_urbanization_list.get(i).get_be();
			// PT
			practices[4] = supporters_rural_practices[4] + (random_generator.nextGaussian() * 20);
			// Conv
			practices[5] = supporters_rural_practices[5] + (random_generator.nextGaussian() * 20);
			
			// Catch outliers.
			for(int j = 0; j < practices.length; j++) {
				// Do not apply the transformation to the value of BE, because it is already in range.
				if(j != 3) {
					if (practices[j] < 0) practices[j] = 0;
					if (practices[j] > 100) practices[j] = 100;
				}
			}
			
			// The speed of a supporter is distributed according to N(6, 3).
			speed = random_generator.nextGaussian() * 3 + 6; 
			// Set the value of speed to zero, if it is below 1.
			if(speed < 1) {
				speed = 0;
			}
			
			// Set the directions of the values of the six dimensions of the practice space.
			directions = new double[practices.length];
			
			// Loop through all dimensions.
			for(int j = 0; j < supporters_rural_directions.length; j++) {
				directions[j] = supporters_rural_directions[j];
			}
			
			// Get the place from the previously defined list.
			double[] place = supporters_urbanization_list.get(i).get_place();
			
			// Set the initial state to "rural".
			String initial_state = "rural";
			
			// Set how strong the decision on the attractivity of the technologies
			// is determined by the supporters neighborhood.
			double neighborhood_influence_factor = 1;
			
			// Create rural_supporter as an instance of the class Supporter.
			rural_supporter =
				new Supporter(random_generator, technologies, supporters, directions, practices, speed, 
						place, initial_state, neighborhood_influence_factor);
			// Add the supporter to the list of supporters.
			supporters.add(rural_supporter);
		}
		
		// Copy the contents of the list supporters to the list supporters_proximity_list
		// to pass it along to the method choose.
		for(int i = 0; i < supporters.size(); i++) {
			supporters_proximity unordered_item = new supporters_proximity();
			unordered_item.set_index(i);
			unordered_item.set_place(supporters.get(i).get_place()); 
			supporters_proximity_list.add(unordered_item);
		}
		
		// Each supporter chooses its technology.
		for(int i = 0; i < supporters.size(); i++) {
			supporters.get(i).update_dissatisfaction(technologies, 0);
			supporters.get(i).choose(technologies, supporters, supporters_proximity_list, 0);
		}
	}

	/**
	 * Calculate a sustainability index. This is unimportant.
	 */
	public void sustainability_index() {
		double practice_mit = 0;
		double practice_pt = 0;
		double average_mit = 0;
		double average_pt = 0;
		
		// Average MIT and PT use.
		for(int i = 0; i < supporters.size(); i++) {
			double[] supporter_practices = new double[technology_ice_practices.length];
			supporter_practices = supporters.get(i).get_practices();
			practice_mit = practice_mit + supporter_practices[0];
			practice_pt = practice_pt + supporter_practices[5];
		}
		average_mit = practice_mit / supporters.size();
		average_pt = practice_pt / supporters.size();
		if(batch_run == false) {
			System.out.println("Average MIT: " + average_mit + ". Average PT: " + average_pt);
		}
	}

	/**
	 * Execute one step of the simulation.
	 */
	@ScheduledMethod(start = 1, interval = 1, priority = 0)
	public void step() {
		step = (int)RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
		
		// Print the technologies in the current step.
		// Only relevant, when the names of technologies can change.
		/*
		System.out.println("technologies.size() = " + technologies.size());
		if(step == 1) {
			for(int i = 0; i < technologies.size(); i++) {
				if(technologies.get(i) != null )
					System.out.println("technologies.get(i).get_name: " + technologies.get(i).get_name());
			}
		} else {
			for(int i = 0; i < summary_output.size(); i++) {
				System.out.println("summary_output.get(i).get(0): " + summary_output.get(i).get(0));
			}
		}
		*/
		
		/*
		 * Normalize the strengths of the technologies.
		 */
		// Declare a variable for the sum of the strengths.
		double sum_strength = 0;
		// Loop through the technologies.
		for(int i = 0; i < technologies.size(); i++) {
			// Exclude non-existing technologies.
			if(technologies.get(i) != null)
				// Add the strength of the currently selected technology to the sum.
				sum_strength = sum_strength + technologies.get(i).get_strength();
		}
		// Loop through the technologies again.
		for(int i = 0; i < technologies.size(); i++) {
			// Exclude non-existing technologies.
			if(technologies.get(i) != null)
				// Set the normalized strength.
				technologies.get(i).set_normalized_strength(sum_strength);
		}
		
		/*
		 * Landscape signals.
		 */
		// Toggle landscape signals on or off.
		if(landscape_signals == true) {
			// The first landscape scenario is the baseline. The baseline
			// is also applied in the policy scenario, because it expresses
			// underlying socio-economic changes.
			if(landscape_scenario == 1 || landscape_scenario == 2 || landscape_scenario == 3) {
				double[] direction = new double[supporters_urban_directions.length];
				
				/* Urbanization */
				if(batch_run == false) {
					System.out.println("Old urbanization rate = " + get_urbanization_rate());
				}
				// 2010 - 2020
				// Yearly increase of 1.4 percentage points, 0.7 percentage points each step.
				if(step >= 20 && step < 40) {
					direction[3] = 0.007;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2020 - 2030
				// Yearly increase of 0.7 percentage points, 0.35 percentage points each step.
				} else if(step >= 40 && step < 60) {
					direction[3] = 0.0035;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2030 - 2040
				// Yearly increase of 0.4 percentage points, 0.2 percentage points each step.
				} else if(step >= 60 && step < 80) {
					direction[3] = 0.002;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2040 - 2050
				// Yearly increase of 0.5 percentage points, 0.25 percentage points each step.
				} else if(step >= 80 && step <= 100) {
					direction[3] = 0.0025;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
				
				if(batch_run == false) {
					System.out.println("new urbanization rate = " + get_urbanization_rate());
				}
				
				// Null the vector to apply other landscape signals
				for(int i = 0; i < direction.length; i++) {
					direction[i] = 0;
				}
				
				/* Income increase */
				// An increase of 0.9375 per step translates into an overall increase of 250%.
				if(step == 20) {
					direction[1] = 0.9375;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
			
			} 
			// The policy scenario.
			if(landscape_scenario == 2) {
				double[] direction = new double[supporters_urban_directions.length];
				
				/* Subsidies */
				// 2010 - 2012
				// Subsidy for BEV and FCV for 20% of the cost
				if(step == 20) {
					// BEV
					double subsidy_percentage = 0.2;
					int subsidy_duration = 4;
					String technology_to_be_manipulated = "BEV";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
					// FCV
					technology_to_be_manipulated = "FCV";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2012 - 2015
				// Subsidy for BEV and FCV for 15% of the cost
				} else if(step == 24) {
					// BEV
					double subsidy_percentage = 0.15;
					int subsidy_duration = 6;
					String technology_to_be_manipulated = "BEV";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
					// FCV
					technology_to_be_manipulated = "FCV";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2015 - 2020
				// Subsidy for BEV and FCV for 10% of the cost
				} else if(step == 30) {
					// BEV
					double subsidy_percentage = 0.1;
					int subsidy_duration = 10;
					String technology_to_be_manipulated = "BEV";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
					// FCV
					technology_to_be_manipulated = "FCV";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
				
				/* Taxes */
				// 2010 - 2020
				// Tax on ICE for 5%
				if(step == 20) {
					// ICE
					double subsidy_percentage = -0.05;
					int subsidy_duration = 20;
					String technology_to_be_manipulated = "ICE";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2020 - 2030
				// Tax on ICE for 7.5%
				} else if(step == 40) {
					double subsidy_percentage = -0.075;
					int subsidy_duration = 20;
					String technology_to_be_manipulated = "ICE";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2030 - 2040
				// Tax on ICE for 10%
				} else if(step == 60) {
					double subsidy_percentage = -0.1;
					int subsidy_duration = 20;
					String technology_to_be_manipulated = "ICE";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2040 - 2050
				// Tax on ICE for 10%
				} else if(step == 80) {
					double subsidy_percentage = -0.1;
					int subsidy_duration = 20;
					String technology_to_be_manipulated = "ICE";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
				
				
				/* Increasing PT */
				// 2010: Increase by 0.2
				if(step == 20) {
					direction[4] = 0.2;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2025: Increase by 0.3
				} else if(step == 50) {
					direction[4] = 0.3;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
				
				// Null the vector to apply other landscape signals
				for(int i = 0; i < direction.length; i++) {
					direction[i] = 0;
				}
				
				/* Decreasing MIT */
				// 2015: Decrease by 0.1
				if(step == 30) {
					direction[2] = -0.05;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				// 2020: Decrease by 0.5
				} else if(step == 40) {
					direction[2] = -0.1;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
				
				// Null the vector to apply other landscape signals
				for(int i = 0; i < direction.length; i++) {
					direction[i] = 0;
				}
				
				/* Decreasing Emissions */
				// 2030: Decrease by 0.1
				if(step == 60) {
					direction[0] = -0.5;
					double subsidy_percentage = 0;
					int subsidy_duration = 0;
					String technology_to_be_manipulated = "";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
			}
			if(landscape_scenario == 3) {
				double[] direction = new double[supporters_urban_directions.length];
			
				/* Sensitivity analysis: Subsidies */
				// Subsidy for BEV and FCV for 10% of the cost
				if(step == 1) {
					// BEV
					double subsidy_percentage = 0.1;
					int subsidy_duration = 100;
					String technology_to_be_manipulated = "BEV";
					landscape_signal(direction, subsidy_duration, subsidy_percentage, technology_to_be_manipulated);
				}
			}
		}
		
		/* 
		 * Supporters
		 */
		// Update supporters_proximity_list in each step after the initialization.
		if(step > 1) {
			supporters_proximity_list.clear();
			// Copy the contents of the list supporters to the list supporters_proximity_list
			// to pass it along to the method choose
			for(int i = 0; i < supporters.size(); i++) {
				supporters_proximity unordered_item = new supporters_proximity();
				unordered_item.set_index(i);
				unordered_item.set_place(supporters.get(i).get_place()); 
				supporters_proximity_list.add(unordered_item);
			}
		}
			
		// Determine, whether a supporter is urban or rural.
		determine_state_of_supporters();
		
		// Randomize the order in which the loop selects the supporters.
		boolean work = true;
		// Declare the array, which contains the order of the supporters.
		int[] random_supporters = new int[supporters.size()];
		
		// Loop through all supporters.
		for(int i = 0; i < random_supporters.length; i++) {
			// Set work to true, to allow the first iteration of the while loop.
			work = true;
			while(work) {
				// Store a random number between 0 and the number of supporters - 1.
				random_supporters[i] = random_generator.nextInt(random_supporters.length);
				// Assume a unique number has been picked and set work to false to abort the loop.
				work = false;
				// Check if there are duplicates by comparing the selected
				// random number to all the previously selected numbers.
				// The loop only has to go up to k < i, because all numbers
				// above i have not been chosen yet.
				for(int k = 0; k < i ; k++) {
					if(random_supporters[i] == random_supporters[k] && i != k) {
						// There are duplicates, so do another iteration of the loop.
						work = true;
					}
				}	
			}
		}
		
		// Use the previously determined order to loop through all supporters.
		for(int i: random_supporters) {
			// random_choosing ~ U(0, 1).
			double random_choosing = random_generator.nextDouble();
			// Update the dissatisfaction levels of the currently selected supporter.
			supporters.get(i).update_dissatisfaction(technologies, step);
			
			// Only 10% of all supporters choose a technology in the current step,
			// so on average a supporter chooses a technology every 10th step.
			if (random_choosing < 0.1) {
				supporters.get(i).choose(technologies, supporters, supporters_proximity_list, step);
			}
					
			// Supporters move in the practice space.
			if(movement == true) {
				supporters.get(i).move();
			}
			
			// Add the output of the currently selected supporter to the output table
			supporters_output.get(i).add(supporters.get(i).toString());
		}
		
		/*
		 * Technologies
		 */
		String state_of_technology;
		// Legacy. Not needed, if technologies do not change names.
		// int number_of_consolidated_technologies = 0;
		
		// Store the current number of technologies.
		int current_number_of_technologies = technologies.size();
		
		// Declare the array, which contains the order of the technologies.
		int[] random_technologies = new int[current_number_of_technologies];
		
		// Loop through all technologies.
		for(int i = 0; i < random_technologies.length; i++) {
			// Set work to true, to allow the first iteration of the while loop.
			work = true;
			while(work) {
				// Store a random number between 0 and the number of technologies - 1.
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
			// True, if the technology still exists.
			if(technologies.get(i) != null) {
				// Calculate the number of supporters for the currently selected technology.
				technologies.get(i).calculate_number_of_supporters(supporters);
								
				// Decide, whether this technology is the regime, an ENA or a niche.
				technologies.get(i).transformation();
				
				// Store the current state of the technology in state_of_technology.
				state_of_technology = technologies.get(i).get_state_of_technology();
								
				// Enable or disable the movement of technologies.
				if(movement == true) {
					// The adaption mechanism depends on the state of the technology
					if(state_of_technology == "regime") {
						technologies.get(i).adaption_regime(supporters, technologies, summary_output, step);
					} else if(state_of_technology == "ena") {
						technologies.get(i).adaption_ena(supporters, technologies, step);
					} else if(state_of_technology == "niche") {
						technologies.get(i).adaption_niche(supporters, technologies, step);
					}
				}
				
				// Calculate the resources of the technology.
				technologies.get(i).calculcate_resources();
				// Calculate the strength of the technology.
				technologies.get(i).calculate_strength();
								
				// Toggling the movement off disables the interaction of technologies.
				if(movement == true) {
					// True, if there has been neither been an absorption or a clustering before.
					if(absorbtion_clustering_count < 1) {
						// Regimes attempts to absorb a niche by proximity.
						if(state_of_technology.equals("regime")) {
							// The return value is either the index of the absorbed technology or
							// -1, indicating no absorption taking place.
							int absorbed_technology = technologies.get(i).absorption_proximity(technologies);
							if(absorbed_technology != -1) {
								// If the absorption has succeeded, the absorbing technology has
								// incorporated the absorbed technology by now. This code block 
								// updates the list of technologies and the output lists.
								
								// The block commented out in the following is an earlier version, which 
								// nulled both technologies and added a new technology to the end of the 
								// list of the technologies with a concatenated name. This unnecessarily 
								// complicates the analysis and has thus been removed again.
								
								/* 
								// Get the name of the absorbed technology.
								String name_of_absorbed_technology = technologies.get(absorbed_technology).get_name();
								// Get the name of the absorbing technology.
								String name_of_absorbing_technology = technologies.get(i).get_name();
								// Concatenated name.
								String name_of_consolidated_technology = name_of_absorbing_technology + "_AS_" + name_of_absorbed_technology;
								
								// Add a new row for the consolidated technology to summary_output and technologies_output.
								summary_output.add(new ArrayList<String>());
								technologies_output.add(new ArrayList<String>());
								// Set the name for the consolidated technology in the last row in the first column.
								summary_output.get(summary_output.size()-1).add(name_of_consolidated_technology);
								technologies_output.get(technologies_output.size()-1).add(name_of_consolidated_technology);
												
								// Remove the absorbed technology from the list of technologies.
								technologies.remove(absorbed_technology);
								// Add null to replace the absorbed technology in the list of technologies.
								technologies.add(absorbed_technology, null);
								// Add the consolidated technology at the end of the list.
								technologies.add(technologies.get(i));
								// Set the name for the consolidated technology.
								technologies.get(technologies.size()-1).set_name(name_of_consolidated_technology);
								// Remove the absorbing technology.
								technologies.remove(i);
								// Add null to replace the absorbing technology.
								technologies.add(i, null);							
								*/
								
								// Move the supporters of the absorbed technology to the absorbing technology.
								for(int j = 0; j < supporters.size(); j++) {
									// True, if the chosen technology of the currently selected supporter is the absorbed technology.
									if(supporters.get(j).get_chosen_technology() == technologies.get(absorbed_technology)) {
										// Set the absorbing technology as the chosen technology of this supporter.
										supporters.get(j).set_chosen_technology(technologies.get(i));
									}
										
								}
								
								// Remove the absorbed technology.
								technologies.remove(absorbed_technology);
								// Add null to replace the absorbed technology in the list of technologies.
								technologies.add(absorbed_technology, null);
								
								// Legacy
								// number_of_consolidated_technologies++;
								
								// Verbosity depends on batch or single run.
								if(batch_run == false) {
									System.out.println("Absorption_proximity.");
								}
								
								// Raise the count by 1.
								absorbtion_clustering_count++;
							}
						// The technology is an EN or niche and tries to cluster.
						// Only execute, if there has been no absorption or clustering before to avoid
						// unrealistic concentration.
						} else if(absorbtion_clustering_count < 1) {
							int clustered_technology = technologies.get(i).clustering(technologies);		
							if(clustered_technology != -1) {
								// Legacy implementation
								/*
								String name_of_clustered_technology = technologies.get(clustered_technology).get_name();
								String name_of_clustering_technology = technologies.get(i).get_name();
								String name_of_consolidated_technology = name_of_clustering_technology + "_CL_" + name_of_clustered_technology;
								
								// Add a new row for the consolidated technology to summary_output and technologies_output.
								summary_output.add(new ArrayList<String>());
								technologies_output.add(new ArrayList<String>());
								// Set the name for the consolidated technology in the last row in the first column.
								summary_output.get(summary_output.size()-1).add(name_of_consolidated_technology);
								technologies_output.get(technologies_output.size()-1).add(name_of_consolidated_technology);
								
								// Remove the absorbed technology from the list of technologies.
								technologies.remove(clustered_technology);
								// Add null to replace the absorbed technology in the list of technologies.
								technologies.add(clustered_technology, null);
								// Add the consolidated technology at the end of the list.
								technologies.add(technologies.get(i));
								// Set the name for the consolidated technology.
								technologies.get(technologies.size()-1).set_name(name_of_consolidated_technology);
								// Remove the clustering technology.
								technologies.remove(i);
								// Add null to replace the clustering technology.
								technologies.add(i, null);							
								*/
								
								// Move the supporters of the absorbed technology to the absorbing technology.
								for(int j = 0; j < supporters.size(); j++) {
									if(supporters.get(j).get_chosen_technology() == technologies.get(clustered_technology)) {
										supporters.get(j).set_chosen_technology(technologies.get(i));
									}
										
								}
								
								// Remove the absorbed technology.
								technologies.remove(clustered_technology);
								// Add null to replace the absorbed technology in the list of technologies.
								technologies.add(clustered_technology, null);
								
								// Legacy
								// number_of_consolidated_technologies++;
								
								if(batch_run == false) {
									System.out.println("Clustering.");
								}
								absorbtion_clustering_count++;
							}	
						}
					} 	
				}
			}
		}
		
		/* Legacy
		// Maintain integrity of summary_output	and technologies_output in case of a new technology	
		while(number_of_consolidated_technologies > 0) {
			// Fill the row of a consolidated technology up unto now with null
			for(int i = 0; i < step; i++) {
				summary_output.get(summary_output.size() - number_of_consolidated_technologies).add(1, "null");
				technologies_output.get(technologies_output.size() - number_of_consolidated_technologies).add(1, toString_technology_null());
			}
			number_of_consolidated_technologies--;
		}
		*/
		
		// Copy data to the output lists.
		for(int i = 0; i < technologies.size(); i++) {
			// Store current number of supporters of this technology.
			if(technologies.get(i) != null) {
				//summary_output.get(i).add(Double.toString(technologies.get(i).get_number_of_supporters()));
				//summary_output.get(i).add(Double.toString(technologies.get(i).get_strength()));
				
				// Save the normalized strengths.
				summary_output.get(i).add(Double.toString(technologies.get(i).get_normalized_strength()));
				// Save all relevant data of all technologies in this step.
				technologies_output.get(i).add(technologies.get(i).toString());
			} else {
				// The technology does not exist anymore, so the outputs are nulled.
				summary_output.get(i).add("null");
				technologies_output.get(i).add(toString_technology_null());
			}
		}
		
		// Verbosity depends on batch or single run.
		if(batch_run == false) {
			System.out.println("Dimensions of summary_output: " + summary_output.size() + " x " + summary_output.get(0).size());
			System.out.println("Dimensions of technologies_output: " + technologies_output.size() + " x " + technologies_output.get(0).size());
			System.out.println("Dimensions of supporters_output: " + supporters_output.size() + " x " + supporters_output.get(0).size());
		}
		
		// Calculate the sustainability index and write the data in the last simulation step.
		// If it does not succeed, print the error.
		if (step == simulation_steps) {
			try {
				sustainability_index();
				this.writeData();
			} catch(IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			// End the run.
			RunEnvironment.getInstance().endRun();
		}
	}	
	
	
	/** Execute a landscape signal.
	 * 
	 * @param direction Vector of landscape signals.
	 * @param subsidy_duration Duration of a subsidy.
	 * @param subsidy_percentage Subsidy as percentage of the underlying price.
	 * @param technology_to_be_manipulated Technology to be subsidized.
	 */
	void landscape_signal(double[] direction, int subsidy_duration, double subsidy_percentage, String technology_to_be_manipulated) {		
		// Loop through all supporters to change their directions.
		for(int i = 0; i < supporters.size(); i++) {
			double[] randomized_direction = new double[direction.length];
			for(int j = 0; j < direction.length; j++) {
				// It is possible to add randomization here.
				randomized_direction[j] = direction[j];
			}
			supporters.get(i).add_to_direction(randomized_direction);
		}
		
		// Check, if there is a BE signal and if so, move one rural supporter to a city,
		// until the target increase in prior to new urbanization rates is reached.
		if(direction[3] != 0) {
			// Store the current urbanization rate.
			double old_urbanization_rate = get_urbanization_rate();
			double new_urbanization_rate;
			double difference_urbanization_rate = 0;
			
			// True, if the urbanization rate can be increased.
			if(old_urbanization_rate < 0.95) {
				// Assume, the first loop iteration suffices.
				boolean work = true;
				while(work == true) {
					// Move one rural supporter to a city.
					be_landscape_signal();
					// Store the new urbanization rate.
					new_urbanization_rate = get_urbanization_rate();
					// Compare the two urbanization rates and abort the loop, if 
					// the difference is big enough.
					difference_urbanization_rate = new_urbanization_rate - old_urbanization_rate;
					if(difference_urbanization_rate >= direction[3]) {
						work = false;
					}
				}
			}
		}
		
		// True, if the size of the manipulation is not equal to zero and
		// if technology_to_be_manipulated is not empty.
		if(subsidy_percentage != 0 && !"".equals(technology_to_be_manipulated)) {
			// Number of technologies, which contain the string stored in
			// technology_to_be_manipulated.
			int technologies_found = 0;
			// ID on the list of technologies of the technology to be manipulated.
			int id_of_technology_to_be_manipulated = -1;
			
			// Check existence of the technology and if it can be unambiguously resolved.
			for(int i = 0; i < technologies.size(); i++) {
				// True, if the technology exists.
				if(technologies.get(i) != null) {
					// True, if the currently selected technology contains technology_to_be_manipulated.
					if(technologies.get(i).get_name().toLowerCase().contains(technology_to_be_manipulated.toLowerCase())) {
						// Raise the counter of technologies, that fit the pattern, by one.
						technologies_found = technologies_found + 1;
						// Set the ID to the last technology found.
						id_of_technology_to_be_manipulated = i;
					}
				}
			}
			
			// True, if the pattern has been found in just one technology name.
			if(technologies_found == 1) {
				// Execute the change.
				technologies.get(id_of_technology_to_be_manipulated).set_subsidy(subsidy_duration, subsidy_percentage, step);
			}
			
		}
	}
	
	/** Translate a BE change to a directed movement on the map. */
	void be_landscape_signal() {
		/*
		 * Select a supporter most similar to the average urban practices.
		 */
		// Array containing the average practices of urban supporters.
		double[] urban_supporters_avg_practices = new double[supporters_urban_practices.length];
		double[] practices_of_supporter = new double[supporters_urban_practices.length];
		int number_of_urban_supporters = 0;
		
		for(int i = 0; i < supporters.size(); i++) {
			if(supporters.get(i).get_state() == "urban") {
				// Raise the counter by 1.
				number_of_urban_supporters++;
				// Store the practices of the currently selected supporter
				// in practices_of_supporter.
				practices_of_supporter = supporters.get(i).get_practices();
				// Loop through all dimensions and sum up the individual practices.
				for(int j = 0; j < urban_supporters_avg_practices.length; j++) {
					urban_supporters_avg_practices[j] = urban_supporters_avg_practices[j] + practices_of_supporter[j];
				}
				
			}
		}
		
		// Verbosity switch.
		if(batch_run == false) {
			System.out.println("Number of urban supporters = " + number_of_urban_supporters);
		}
		
		// Divide by the number of urban supporters to get the average values.
		for(int i = 0; i < urban_supporters_avg_practices.length; i++) {
			urban_supporters_avg_practices[i] = urban_supporters_avg_practices[i] / number_of_urban_supporters;
		}
		
		// Calculate the similarity between a rural supporter and the average values of urban supporters.
		int selected_rural_supporter = -1;
		double distance = Double.POSITIVE_INFINITY;
		// Loop through all supporters.
		for(int i = 0; i < supporters.size(); i++) {
			double tmp_distance;
			// True, if the supporter is a rural supporter
			if(supporters.get(i).get_state() == "rural") {
				double[] tmp_practices;
				tmp_distance = 0;
				// Get the current practices.
				tmp_practices = supporters.get(i).get_practices();
				// Loop through the practices and calculate the distance.
				for(int j = 0; j < tmp_practices.length; j++) {
					tmp_distance = tmp_distance + 
							(urban_supporters_avg_practices[j] - tmp_practices[j]) * (urban_supporters_avg_practices[j] - tmp_practices[j]);
				}
				tmp_distance = Math.sqrt(tmp_distance);
				// Set selected_rural_supporter to the index of the supporter in the list supporter.
				if(tmp_distance < distance) {
					distance = tmp_distance;
					selected_rural_supporter = i;
				}
			}
		}
		
		// Verbosity switch.
		if(batch_run == false) {
			System.out.println("selected_rural_supporter = " + selected_rural_supporter);
		}
		
		// Find this supporter in supporters_urbanization_list.
		int selected_rural_supporter_urbanization = -1;
		for(int i = 0; i < supporters_urbanization_list.size(); i++) {
			if(supporters_urbanization_list.get(i).get_index() == selected_rural_supporter) {
				// Found this rural supporter.
				selected_rural_supporter_urbanization = i;
			}
		}
		
		// Verbosity switch.
		if(batch_run == false) {
			System.out.println("selected_rural_supporter_urbanization = " + selected_rural_supporter_urbanization);
		}
		
		// x and y coordinates of the place of the supporter.
		double[] place_original = {0, 0};
		// Scaled coordinates of the supporter.
		double[] place_scaled = {0, 0};
		
		// Store the original place.
		place_original = supporters_urbanization_list.get(selected_rural_supporter_urbanization).get_place();
		
		// Math.sqrt(population_density_resolution) is the edge length of the map.
		place_scaled[0] = place_original[0] / (int)(Math.sqrt(population_density_resolution));
		place_scaled[1] = place_original[1] / (int)(Math.sqrt(population_density_resolution));
		
		// Cast the double to int, so as to be usable as an array index.
		int x_coordinate_density_map = (int)(place_scaled[0]);
		// Lower the coordinate by one or it could be out of bound for the array.
		if(x_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
			x_coordinate_density_map = x_coordinate_density_map - 1;
		}
		// Repeat the previous steps for the other coordinate.
		int y_coordinate_density_map = (int)(place_scaled[1]);
		if(y_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
			y_coordinate_density_map = y_coordinate_density_map - 1;
		}
		
		// Move to cell, which has a BE value above the threshold
		double tmp_distance = 0;
		double distance_btw_supporter_and_cell = Double.POSITIVE_INFINITY;
		int[] result = {-1, -1};
		
		// Distance from current position to all cells, order cells with distance ascending, subtract rural cells
		// and cells with the maximum BE value, i.e. maximum population.
		for(int row = 0; row < density_map.length; row++) {
			 for(int col = 0; col < density_map[row].length; col++) {
				 // Exclude the cell in which the supporter is located.
				 if(!(row == x_coordinate_density_map && col == y_coordinate_density_map)) {
					 // Exclude the cells, which are rural and those with maximum BE.
					 if(density_map[row][col] >= frontier_urban_rural && density_map[row][col] < max_be) {
						 // Order the cells based on distance to the position of the supporter in ascending order.
						 tmp_distance = (x_coordinate_density_map - row) * (x_coordinate_density_map - row) +
								 (y_coordinate_density_map - col) * (y_coordinate_density_map - col);
						 tmp_distance = Math.sqrt(tmp_distance);
						 if(tmp_distance < distance_btw_supporter_and_cell) {
							 result[0] = row;
							 result[1] = col;
							 distance_btw_supporter_and_cell = tmp_distance;
						 }
					 }
				 }
			 }
		}
		
		 // Suppress output in batch runs.
		 if(batch_run == false) {
			 System.out.println("Closest cell: " + result[0] + result[1]); 
		 }
		 
		 // Fallback: All urban cells have reached max_be. Increase the population in the most populated cells.
		 if(result[0] == -1 || result [1] == -1 && false) {
			 System.out.println("Executing fallback urbanization mechanism.");
			 for(int row = 0; row < density_map.length; row++) {
				 for(int col = 0; col < density_map[row].length; col++) {
					 // Exclude the cell in which the supporter is located.
					 if(!(row == x_coordinate_density_map && col == y_coordinate_density_map)) {
						 // Exclude the cells, which are rural.
						 if(density_map[row][col] >= frontier_urban_rural) {
							 // Order the cells based on distance to the position of the supporter in ascending order.
							 tmp_distance = (x_coordinate_density_map - row) * (x_coordinate_density_map - row) +
									 (y_coordinate_density_map - col) * (y_coordinate_density_map - col);
							 tmp_distance = Math.sqrt(tmp_distance);
							 if(tmp_distance < distance_btw_supporter_and_cell) {
								 result[0] = row;
								 result[1] = col;
								 distance_btw_supporter_and_cell = tmp_distance;
							 }
						 }
					 }
				 }
			}
		 }
		 
	 	 // Move.
		 double[] old_position = {0, 0};
		 double[] new_position = {0, 0};
		 
		 // Get the old/current position.
		 old_position = supporters_urbanization_list.get(selected_rural_supporter_urbanization).get_place();
		 // Scale the position and add a random number between 0 and 9.
		 new_position[0] = result[0] * 10 + random_generator.nextInt(10);
		 new_position[1] = result[1] * 10 + random_generator.nextInt(10);
		 // Set the new position.
		 supporters_urbanization_list.get(selected_rural_supporter_urbanization).set_place(new_position);
		
		// Recreate the density map.
		create_density_map();
		// Update the BE values for all supporters.
		determine_be();

		// Copy BE values back to supporters list.
		for(int i = 0; i < supporters_urbanization_list.size(); i++) {
			// The index of each element of supporters_urbanization_list corresponds with the order of
			// the list supporters.
			double[] practices = supporters.get(supporters_urbanization_list.get(i).get_index()).get_practices();
			double[] new_practices = practices;
			double[] new_place = new double[2];
			new_practices[3] = supporters_urbanization_list.get(i).get_be();
			// Store the new value in the list supporters.
			supporters.get(supporters_urbanization_list.get(i).get_index()).set_practices(new_practices);
			new_place = supporters_urbanization_list.get(i).get_place();
			// Store the new place in the list supporters.
			supporters.get(supporters_urbanization_list.get(i).get_index()).set_place(new_place);
		}
		
		// Verbosity switch.
		if(batch_run == false) {
			System.out.println("BEFORE selected_rural_supporter state = " + supporters.get(selected_rural_supporter).get_state());
		}
		
		// Update the state of all supporters: "urban" or "rural".
		determine_state_of_supporters();
		
		// Verbosity switch.
		if(batch_run == false) {
			System.out.println("AFTER selected_rural_supporter state = " + supporters.get(selected_rural_supporter).get_state());
		}
	}
	
	/** Populate the density map. */
	void create_density_map() {
		// The original place of the supporter on the map.
		double[] place_original = {0, 0};
		// The scaled place of the supporter on the map is the cell in which it belongs.
		double[] place_scaled = {0, 0};

		// Empty the density map.
		 for(int row = 0; row < density_map.length; row++) {
			 for(int col = 0; col < density_map[row].length; col++) {
				 density_map[row][col] = 0;
			 }
		 }
		
		// Loop through all supporters and populate the density map.
		for(int i = 0; i < supporters_urbanization_list.size(); i++) {
			// Get the original place
			place_original = supporters_urbanization_list.get(i).get_place();
			
			// Scale the coordinates to correspond to the cells.
			place_scaled[0] = place_original[0] / (int)(Math.sqrt(population_density_resolution));
			place_scaled[1] = place_original[1] / (int)(Math.sqrt(population_density_resolution));
			
			// Check, if the scaled place is within range. If not, reduce it by one.
			int x_coordinate_density_map = (int)(place_scaled[0]);
			if(x_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
				x_coordinate_density_map = x_coordinate_density_map - 1;
			}
			// Check it again for the other coordinate.
			int y_coordinate_density_map = (int)(place_scaled[1]);
			if(y_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
				y_coordinate_density_map = y_coordinate_density_map - 1;
			}
			
			// Increase the counter on the density map by one.
			density_map[x_coordinate_density_map][y_coordinate_density_map] = 
					density_map[x_coordinate_density_map][y_coordinate_density_map] + 1;	
		}
	}
	
	/** Determine the BE value for each supporter. */
	void determine_be() {
		// The original place of the supporter on the map.
		double[] place_original = {0, 0};
		// The scaled place of the supporter on the map is the cell in which it belongs.
		double[] place_scaled = {0, 0};
		// Loop through all supporters again to set the values for BE.
		for(int i = 0; i < supporters_urbanization_list.size(); i++) {
			// Store the original place in place_original.
			place_original = supporters_urbanization_list.get(i).get_place();
			
			// Scale the coordinates to correspond to the cells.
			place_scaled[0] = place_original[0] / (int)(Math.sqrt(population_density_resolution));
			place_scaled[1] = place_original[1] / (int)(Math.sqrt(population_density_resolution));
			
			// Check, if the scaled place is within range. If not, reduce it by one.
			int x_coordinate_density_map = (int)(place_scaled[0]);
			if(x_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
				x_coordinate_density_map = x_coordinate_density_map - 1;
			}
			// Check it again for the other coordinate.
			int y_coordinate_density_map = (int)(place_scaled[1]);
			if(y_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
				y_coordinate_density_map = y_coordinate_density_map - 1;
			}
			
			// Get the density for this supporter from the density map.
			double density = (double)(density_map[x_coordinate_density_map][y_coordinate_density_map]);
			
			// Pass the density in the supporter's cell along as BE.
			supporters_urbanization_list.get(i).set_be(density);
		}
		
		// Sort the supporters from smallest BE value to biggest.
		Collections.sort(supporters_urbanization_list);
		// Reverse the order, from biggest to smallest.
		Collections.reverse(supporters_urbanization_list);
		
		// Scale BE to 0-100.
		// Only execute, if the biggest value of BE is > 100.
		if(supporters_urbanization_list.get(0).get_be() > 100 && step >= 1) {
			// Get the scale factor.
			be_scale_factor = 100 / supporters_urbanization_list.get(0).get_be();
			// Declare a variable for the scaled BE value.
			double scaled_be;
			
			// Loop through all supporters to apply the scaling.
			for(int i = 0; i < supporters_urbanization_list.size(); i++) {
				// Apply the scale factor.
				scaled_be = be_scale_factor * supporters_urbanization_list.get(i).get_be();
				// Save the scaled value of BE.
				supporters_urbanization_list.get(i).set_be(scaled_be);
			}
		}
		
		// The maximum value of BE is that of the first supporter on the list.
		max_be_scaled = supporters_urbanization_list.get(0).get_be();	
	}
	
	/** Set the state to "urban" or "rural" for all supporters. */
	void determine_state_of_supporters() {
		double[] place_original = {0, 0};
		double[] place_scaled = {0, 0};
		
		for(int i = 0; i < supporters.size(); i++) {
			place_original = supporters.get(i).get_place();
			
			place_scaled[0] = place_original[0] / (int)(Math.sqrt(population_density_resolution));
			place_scaled[1] = place_original[1] / (int)(Math.sqrt(population_density_resolution));
			
			int x_coordinate_density_map = (int)(place_scaled[0]);
			if(x_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
				x_coordinate_density_map = x_coordinate_density_map - 1;
			}
			
			int y_coordinate_density_map = (int)(place_scaled[1]);
			if(y_coordinate_density_map == (int)(Math.sqrt(population_density_resolution))) {
				y_coordinate_density_map = y_coordinate_density_map - 1;
			}
			
			// Get the density for this supporter from the density map
			double density = (double)(density_map[x_coordinate_density_map][y_coordinate_density_map]);
			
			String previous_state = supporters.get(i).get_state();
			
			// Set the state in relation to the initially set frontier between rural and urban supporters.
			if(density >= frontier_urban_rural) {
				supporters.get(i).set_state("urban");
			} else if(density < frontier_urban_rural) {
				supporters.get(i).set_state("rural");
			}
			
			String current_state = supporters.get(i).get_state();
			if(batch_run == false && previous_state == "urban" && current_state == "rural") {
				// Permissible during the initialization.
				System.out.println("Supporter " + i + " changed from urban to rural.");
			}
		}	
	}
	
	/** Get the urbanization rate .
	 * @return Urbanization rate: [0, 1] */
	double get_urbanization_rate() {
		double urbanization_rate = 0;
		double urban_supporter_counter = 0;
		// Loop through all supporters and count the urban supporters.
		for(int i = 0; i < supporters.size(); i++) {
			if(supporters.get(i).get_state() == "urban") {
				urban_supporter_counter++;
			}
		}
		// Compute the urbanization rate.
		urbanization_rate = urban_supporter_counter / supporters.size();
		// Return the urbanization rate.
		return urbanization_rate;
	}
	
	/** Print the output */
	public void writeData() throws IOException {
		/* If batch == true:
		 * write technologies_batch_output.csv
		 */
		
		if(logging == true) {
			if(batch_run == false) {
				//File summary = new File("./output/"+"output"+name+country+"dcMIV"+supporters_car_direction_mit+"dcOEV"+supporters_car_direction_pt+"dcICT"+supporters_car_direction_ict+"dcBE"+supporters_car_direction_be+"dgcMIV"+supporters_greencar_direction_mit+"dgcOEV"+supporters_greencar_direction_pt+"dgcICT"+supporters_greencar_direction_ict+"dgcBE"+supporters_greencar_direction_be+"dsMIV"+this.directionslowMIV+"dsOEV"+this.directionslowOEV+"dsICT"+this.directionslowICT+"dsBE"+this.directionslowBuildenv+"dpubMIV"+this.directionpublicMIV+"dpubOEV"+this.directionpublicOEV+"dpubICT"+this.directionpublicICT+"dpubBE"+this.directionpublicBuildenv+".csv");
				File summary = new File("./output/summary_output.csv");
				//File complete = new File("./output/"+"complete"+name+country+"dcMIV"+supporters_car_direction_mit+"dcOEV"+supporters_car_direction_pt+"dcICT"+supporters_car_direction_ict+"dcBE"+supporters_car_direction_be+"dgcMIV"+supporters_greencar_direction_mit+"dgcOEV"+supporters_greencar_direction_pt+"dgcICT"+directiongreencarICT+"dgcBE"+supporters_greencar_direction_be+"dsMIV"+this.directionslowMIV+"dsOEV"+this.directionslowOEV+"dsICT"+this.directionslowICT+"dsBE"+this.directionslowBuildenv+"dpubMIV"+this.directionpublicMIV+"dpubOEV"+this.directionpublicOEV+"dpubICT"+this.directionpublicICT+"dpubBE"+this.directionpublicBuildenv+".csv");
				File complete = new File("./output/technologies_output.csv");
				File supporters = new File("./output/supporters_output.csv");
				
				FileWriter fw_summary = new FileWriter(summary, false);
				FileWriter fw_complete = new FileWriter(complete, false);
				FileWriter fw_supporters = new FileWriter(supporters, false); 
				
				/* Write summary_output */
				
				// Print the headline: year, tech_1, tech_2, ... tech_n
				fw_summary.write("year ");
				for(int i = 0; i < summary_output.size(); i++) {
					fw_summary.write(summary_output.get(i).get(0) + " ");
				}
				fw_summary.write("\n");
				// For each time step print the number of supporters for each technology 
				for(int i = 1; i <= simulation_steps ; i++) {
					fw_summary.write((i) + " ");
					for(int j = 0; j < summary_output.size(); j++) {
						fw_summary.write(summary_output.get(j).get(i) + " ");	
					}
					fw_summary.write("\r\n");
				}
				
				
				/* Write technologies_output */
			
				fw_complete.write("year,");
							
				for(int i = 0; i < technologies_output.size(); i++) {
					fw_complete.write("name_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("state_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("number_supporters_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("strength_" + technologies_output.get(i).get(0) + ",");
					//fw_complete.write("PC_" + technologies_output.get(i).get(0) + ",");
					
					fw_complete.write("practice_co2_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_cost_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_mit_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_be_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_pt_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_conv_" + technologies_output.get(i).get(0) + ",");
									
					fw_complete.write("practice_direction_co2_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_cost_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_mit_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_be_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_pt_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_conv_" + technologies_output.get(i).get(0) + ",");
				}	
				fw_complete.write("\n");
				
				for(int i = 1; i <= simulation_steps; i++) {
					fw_complete.write((i) + ",");
					for(int j = 0; j < technologies_output.size(); j++) {
						fw_complete.write(technologies_output.get(j).get(i));
					}
					fw_complete.write("\n");
				}
			
				/* Write supporters_output */
		
				fw_supporters.write("year,");
	
				for(int i = 1; i <= supporters_output.size(); i++) {
					fw_supporters.write("chosen_technology_" + i + ",");
					fw_supporters.write("practice_co2_" + i + ",");
					fw_supporters.write("practice_cost_" + i + ",");
					fw_supporters.write("practice_mit_" + i + ",");
					fw_supporters.write("practice_be_" + i + ",");
					fw_supporters.write("practice_pt_" + i + ",");
					fw_supporters.write("practice_conv_" + i + ",");
					
					fw_supporters.write("practice_direction_co2_" + i + ",");
					fw_supporters.write("practice_direction_cost_" + i + ",");
					fw_supporters.write("practice_direction_mit_" + i + ",");
					fw_supporters.write("practice_direction_be_" + i + ",");
					fw_supporters.write("practice_direction_pt_" + i + ",");
					fw_supporters.write("practice_direction_conv_" + i + ",");
					
					fw_supporters.write("x_coordinate_" + i + ",");
					fw_supporters.write("y_coordinate_" + i + ",");
					
					fw_supporters.write("speed_" + i + ",");
					
					fw_supporters.write("initial_category_" + i + ",");
					fw_supporters.write("state_" + i + ",");
				}		
				
				fw_supporters.write("\n");
				
				for(int i = 0; i < simulation_steps; i++) {
					fw_supporters.write(i + 1 + ",");;
					for(int j = 0; j < supporters_output.size(); j++) {
						fw_supporters.write(supporters_output.get(j).get(i));
					}
					fw_supporters.write("\n");
				}
				
				/* Write supporters: PC and IC */
				fw_summary.flush();
				fw_summary.close();
						
				fw_complete.flush();
				fw_complete.close();
				
				fw_supporters.flush();
				fw_supporters.close();
				
				// Create a diagram with the normalized strengths over time.
				// Has to be commented out in batch runs or java.lang.NoClassDefFoundError is thrown.
				/*
				final Print_diagram diagram = 
					new Print_diagram("Percentage supporter", summary_output, simulation_steps);
				diagram.pack();
				RefineryUtilities.centerFrameOnScreen(diagram);
				diagram.setVisible(true);
				*/
			} else if(batch_run == true) {
				// Set the filenames.
				File summary = new File("summary_output_landscapescenario_" + landscape_scenario + "_random_seed_" + RandomHelper.getSeed()+ ".csv");
				File complete = new File("technologies_output_landscapescenario_" + landscape_scenario + "_random_seed_" + RandomHelper.getSeed()+ ".csv");
				File supporters = new File("supporters_output_landscapescenario_" + landscape_scenario + "_random_seed_" + RandomHelper.getSeed()+ ".csv");
				
				//File supporters = new File("./output/supporters_output_landscapescenario_" + landscape_scenario + "_rndseed_" + RandomHelper.getSeed() + ".csv");
				
				FileWriter fw_summary = new FileWriter(summary, false);
				FileWriter fw_complete = new FileWriter(complete, false);
				FileWriter fw_supporters = new FileWriter(supporters, false);
				
				/* Write summary_output */
				// Print meta data
				
				//DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");  
				//LocalDateTime now = LocalDateTime.now();
				
				//fw_summary.write("landscape_scenario=" + landscape_scenario + "//random_seed=" + RandomHelper.getSeed());  
				//fw_summary.write("\n");
				
				// Print the headline: year, tech_1, tech_2, ... tech_n
				fw_summary.write("year ");
				for(int i = 0; i < summary_output.size(); i++) {
					fw_summary.write(summary_output.get(i).get(0) + " ");
				}
				fw_summary.write("\n");
				// For each time step print the number of supporters for each technology 
				for(int i = 1; i <= simulation_steps ; i++) {
					fw_summary.write((i) + " ");
					for(int j = 0; j < summary_output.size(); j++) {
						fw_summary.write(summary_output.get(j).get(i) + " ");	
					}
					fw_summary.write("\r\n");
				}
				
				/* Write technologies_output */
				fw_complete.write("year,");
				
				for(int i = 0; i < technologies_output.size(); i++) {
					fw_complete.write("name_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("state_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("number_supporters_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("strength_" + technologies_output.get(i).get(0) + ",");

					fw_complete.write("practice_co2_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_cost_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_mit_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_be_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_pt_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_conv_" + technologies_output.get(i).get(0) + ",");
									
					fw_complete.write("practice_direction_co2_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_cost_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_mit_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_be_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_pt_" + technologies_output.get(i).get(0) + ",");
					fw_complete.write("practice_direction_conv_" + technologies_output.get(i).get(0) + ",");
				}	
				fw_complete.write("\n");
				
				for(int i = 1; i <= simulation_steps; i++) {
					fw_complete.write((i) + ",");
					for(int j = 0; j < technologies_output.size(); j++) {
						fw_complete.write(technologies_output.get(j).get(i));
					}
					fw_complete.write("\n");
				}
				
				/* Write supporters_output */
				
				fw_supporters.write("year,");
	
				for(int i = 1; i <= supporters_output.size(); i++) {
					fw_supporters.write("chosen_technology_" + i + ",");
					fw_supporters.write("practice_co2_" + i + ",");
					fw_supporters.write("practice_cost_" + i + ",");
					fw_supporters.write("practice_mit_" + i + ",");
					fw_supporters.write("practice_be_" + i + ",");
					fw_supporters.write("practice_pt_" + i + ",");
					fw_supporters.write("practice_conv_" + i + ",");
					
					fw_supporters.write("practice_direction_co2_" + i + ",");
					fw_supporters.write("practice_direction_cost_" + i + ",");
					fw_supporters.write("practice_direction_mit_" + i + ",");
					fw_supporters.write("practice_direction_be_" + i + ",");
					fw_supporters.write("practice_direction_pt_" + i + ",");
					fw_supporters.write("practice_direction_conv_" + i + ",");
					
					fw_supporters.write("x_coordinate_" + i + ",");
					fw_supporters.write("y_coordinate_" + i + ",");
					
					fw_supporters.write("speed_" + i + ",");
					
					fw_supporters.write("initial_category_" + i + ",");
					fw_supporters.write("state_" + i + ",");
				}		
				
				fw_supporters.write("\n");
				
				for(int i = 0; i < simulation_steps; i++) {
					fw_supporters.write(i + 1 + ",");;
					for(int j = 0; j < supporters_output.size(); j++) {
						fw_supporters.write(supporters_output.get(j).get(i));
					}
					fw_supporters.write("\n");
				}
				
				
				
				fw_summary.flush();
				fw_summary.close();
				
				fw_complete.flush();
				fw_complete.close();
				
				fw_supporters.flush();
				fw_supporters.close();
			}	
		}

		// Clear all the lists after each run.
		summary_output.clear();
		technologies_output.clear();
		supporters_output.clear();	
		
		supporters.clear();
		technologies.clear();
		
		supporters_proximity_list.clear();
		supporters_urbanization_list.clear();
		
		System.out.println("Executed run successfully." );
	}
	
	/** Return nulls for the output tables */
	public String toString_technology_null() {
		int number_of_nulls = 16;
		String return_string = "";
		
		for(int i = 0; i < number_of_nulls; i++) {
			return_string = return_string + "null,";
		}
		return return_string;
	}
}

