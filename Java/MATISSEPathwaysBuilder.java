package matisse;

// Import Repast Simphony libraries.
import repast.simphony.context.Context;
import repast.simphony.dataLoader.ContextBuilder;
import repast.simphony.parameter.Parameters;
import repast.simphony.engine.environment.RunEnvironment;

public class MATISSEPathwaysBuilder implements ContextBuilder<Object>  {
	/* (non-Javadoc)
	 * @see repast.simphony.dataLoader.ContextBuilder#build(repast.simphony.context.Context)
	 */
	@Override
	public Context build(Context<Object> context) {
		/*
		 * Set general parameters needed to run simulations.
		 */
		// Set the ID.
		context.setId("matisse");
		
		// Get the parameters set in the GUI of the simulation.
		Parameters params = (Parameters) RunEnvironment.getInstance().getParameters();
		
		String name = (String)params.getValue("name");
		
		// Set the number of simulation steps.
		// Each simulation step represents six months and the first simulation step
		// corresponds with 2010. In the analysis, the first 20 steps are considered
		// to be the "burn-in phase" and are discarded.
		int simulation_steps = 100;
		
		// If logging is on, the simulation stores outputs externally.
		boolean logging = (boolean)params.getValue("logging");
		
		// Disable or enable the movement of supporters and technologies.
		boolean movement = (boolean)params.getValue("movement");
		
		// Disable or enable landscape signals
		boolean landscape_signals = (boolean)params.getValue("landscape_signals");
		
		// Three landscape scenarios are selectable by this parameter.
		// 1: Baseline
		// 2: Policy
		int landscape_scenario = (int)params.getValue("landscape_scenario");
		
		// Adapt the model to batch runs by lowering the verbosity (console output, diagram)
		// and changing the output path.
		boolean batch_run = (boolean)params.getValue("batch_run");
		
		/*
		 * Set the initial parameters of all technologies.
		 */
		
		// Set the initial parameters of the technology ICE.
		// Practices: Emissions, cost, MIT, BE, PT, Conv.
		double[] technology_ice_practices = {90, 50, 78, 50, 5, 80};
		double[] technology_ice_directions = {0, 0, 0, 0, 0, 0};
		double[][] technology_ice_frontiers =
			{{50, 90}, {40, 100}, {60, 100}, {0, 60}, {0, 30}, {50, 100}};
		// Set the initial initial strength.
		double technology_ice_strength = 100;

		// Set the initial parameters of the technology FCV.
		// Practices: Emissions, cost, MIT, BE, PT, Conv.
		double[] technology_fcv_practices = {100, 90, 78, 90, 5, 50};
		double[] technology_fcv_directions = {0, 0, 0, 0, 0, 0};
		double[][] technology_fcv_frontiers =
			{{30, 100}, {40, 100}, {60, 100}, {20, 90}, {0, 30}, {50, 100}};
		// Set the initial initial strength.
		double technology_fcv_strength = 0.001;

		// Set the initial parameters of the technology BEV.
		// Practices: Emissions, cost, MIT, BE, PT, Conv.
		double[] technology_bev_practices = {62.5, 90, 40, 80, 5, 60};
		double[] technology_bev_directions = {0, 0, 0, 0, 0, 0};
		double[][] technology_bev_frontiers =
			{{20, 70}, {30, 100}, {20, 80}, {15, 80}, {0, 30}, {40, 80}};
		// Set the initial initial strength.
		double technology_bev_strength = 0.001;

		// Set the initial parameters of the technology Public transport.
		// Practices: Emissions, cost, MIT, BE, PT, Conv.
		double[] technology_publictransport_practices = {25, 10, 3, 50, 30, 50};
		double[] technology_publictransport_directions = {0, 0, 0, 0, 0, 0} ;
		double[][] technology_publictransport_frontiers =
			{{1, 30}, {5, 25}, {0, 15}, {15, 100}, {20, 100}, {10, 60}};
		// Set the initial initial strength.
		double technology_publictransport_strength = 25;
		
		// Set the initial parameters of the technology Slow modes.
		// Practices: Emissions, cost, MIT, BE, PT, Conv.
		double[] technology_slowmodes_practices = {20, 5, 6, 50, 2, 30};
		double[] technology_slowmodes_directions = {0, 0, 0, 0, 0, 0};
		double[][] technology_slowmodes_frontiers =
			{{1, 25}, {1, 20}, {0, 30}, {5, 100}, {2, 50}, {5, 30}};
		// Set the initial initial strength.
		double technology_slowmodes_strength = 50;
		
		/*
		 * Set the initial parameters of all supporters.
		 */
		
		// Set the number of supporters.
		int number_of_supporters = 1000;
		double initial_urbanization_rate = 0.49;
		
		// Set the initial parameters for the urban supporter group.
		// Practices: Emissions, Cost, MIT, BE, PT, Conv.
		double[] supporters_urban_practices = {70, 40, 70, 0, 20, 60};
		// The initial directions for Cost, MIT and PT can be set externally. 
		double[] supporters_urban_directions = {(Double)params.getValue("supporters_urban_direction_co2"), 
				(Double)params.getValue("supporters_urban_direction_cost"), 
				(Double)params.getValue("supporters_urban_direction_mit"), 0,
				(Double)params.getValue("supporters_urban_direction_pt"), 
				(Double)params.getValue("supporters_urban_direction_conv")};
		
		// Set the initial parameters for the rural supporter group
		// Practices: Emissions, cost, MIT, BE, PT, Conv
		double[] supporters_rural_practices = {45, 20, 20, 0, 20, 30};
		// The initial directions for Cost, MIT and PT can be set externally. 
		double[] supporters_rural_directions = {(Double)params.getValue("supporters_rural_direction_co2"), 
				(Double)params.getValue("supporters_rural_direction_cost"), 
				(Double)params.getValue("supporters_rural_direction_mit"), 0,
				(Double)params.getValue("supporters_rural_direction_pt"), 
				(Double)params.getValue("supporters_rural_direction_conv")};		
		
		// Create the object supporter as instance of the class Run_supporter
		// to access the members (methods and variables) of that class.
		// The methods and variables are to be found in Run_supporter.java
		Run_supporter supporter = new Run_supporter();
		
		// Pass along general parameters to the object supporter.
		supporter.set_name(name);
		supporter.set_simulation_steps(simulation_steps);
		supporter.set_logging(logging);
		supporter.set_landscape_scenario(landscape_scenario);
		supporter.set_movement(movement);
		supporter.set_landscape_signals(landscape_signals);
		supporter.set_batch_run(batch_run);
		
		// Pass along the parameters of the technologies to the object supporter.
		supporter.set_technology_ice_parameters(technology_ice_practices, technology_ice_directions, 
				technology_ice_frontiers, technology_ice_strength);
		supporter.set_technology_fcv_parameters(technology_fcv_practices, technology_fcv_directions, 
				technology_fcv_frontiers, technology_fcv_strength);
		supporter.set_technology_bev_parameters(technology_bev_practices, technology_bev_directions, 
				technology_bev_frontiers, technology_bev_strength);
		supporter.set_technology_publictransport_parameters(technology_publictransport_practices, technology_publictransport_directions, 
				technology_publictransport_frontiers, technology_publictransport_strength);
		supporter.set_technology_slowmodes_parameters(technology_slowmodes_practices, technology_slowmodes_directions, 
				technology_slowmodes_frontiers, technology_slowmodes_strength);
		
		// Pass along the parameters of both supporter groups to the object supporter.
		supporter.set_number_supporters(number_of_supporters);
		supporter.set_initial_urbanization_rate(initial_urbanization_rate);
		
		supporter.set_supporters_urban_practices(supporters_urban_practices);
		supporter.set_supporters_urban_directions(supporters_urban_directions);
		
		supporter.set_supporters_rural_practices(supporters_rural_practices);
		supporter.set_supporters_rural_directions(supporters_rural_directions);
		
		// Execute the member method of supporter.
		// See Run_supporter.java
		supporter.initialisation();

		
		// Repast Simphony internals.
		context.add(supporter);
		return context;
	}
}
