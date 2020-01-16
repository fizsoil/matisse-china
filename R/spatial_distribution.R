#################################################
# Get the coordinates of supporters of one run  #
# in the first and last step and and plot them  #
# on two maps. The colour of the dots depends   #
# on their state (rural/urban).                 #
#################################################

#####
# Initialization
#####
# Clear everything.
rm(list=ls())

# Load packages.
require("ggplot2")
require("data.table")

# Set the working directory.
setwd("C:/Users/moritz/Google\ Drive/RUB/Masterarbeit/MATISSE\ workspace/Analysis")
# Set the number of threads. Should match the number of logical CPUs.
setDTthreads(4)

# Set the file to be analyzed.
input <- "../matisse/output/supporters_output.csv"
# Set the output directory.
output_directory <- "../../Latex/pictures/"
# Set the name of the plot to be produced.
output_filename <- "spatial_distribution.png"

# Read the file.
supporters_output <- fread(input, header=TRUE)

# Drop the last column, which is empty.
supporters_output[,dim(supporters_output)[2]:=NULL]

#####
# Data operations
#####
# Get the x coordinates in the first step.
x_coordinates_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("x_coordinate_", names(supporters_output))]
x_coordinates_step_0 <- transpose(x_coordinates_step_0)
setnames(x_coordinates_step_0, "x_coordinate")

# Get the y coordinates in the first step.
y_coordinates_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("y_coordinate_", names(supporters_output))]
y_coordinates_step_0 <- transpose(y_coordinates_step_0)
setnames(y_coordinates_step_0, "y_coordinate")

# Get the states in the first step.
state_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("state_", names(supporters_output))]
state_0 <- transpose(state_0)
setnames(state_0, "state")

# Bind everything together.
xy_coordinates_state_step_0 <- cbind(state_0, x_coordinates_step_0, y_coordinates_step_0)

# Plot it.
gg_xy_coordinates_state_step_0 <- ggplot(xy_coordinates_state_step_0, aes(x=x_coordinate, y=y_coordinate, colour=state)) + 
  geom_point() + 
  scale_x_continuous(breaks = seq(0, 100, 10)) +
  scale_y_continuous(breaks = seq(0, 100, 10)) +
  theme(plot.title = element_text(hjust = 0.5)) +
  labs(colour = "State", x = "x", y = "y", title="A") +
  theme(legend.position="none")

#print(gg_xy_coordinates_state_step_0)

# Get the x coordinates in the last step.
x_coordinates_step_99 <- supporters_output[year==99, print(.SD), .SDcols = grep("x_coordinate_", names(supporters_output))]
x_coordinates_step_99 <- transpose(x_coordinates_step_99)
setnames(x_coordinates_step_99, "x_coordinate")

# Get the y coordinates in the last step.
y_coordinates_step_99 <- supporters_output[year==99, print(.SD), .SDcols = grep("y_coordinate_", names(supporters_output))]
y_coordinates_step_99 <- transpose(y_coordinates_step_99)
setnames(y_coordinates_step_99, "y_coordinate")

# Get the states in the last step.
step_99 <- supporters_output[year==100, print(.SD), .SDcols = grep("state_", names(supporters_output))]
step_99 <- transpose(step_99)
setnames(step_99, "final_state")

# Bind everything together.
xy_coordinates_state_step_99 <- cbind(step_99, x_coordinates_step_99, y_coordinates_step_99)

# Plot it.
gg_xy_coordinates_state_step_99 <- ggplot(xy_coordinates_state_step_99, aes(x=x_coordinate, y=y_coordinate, colour=final_state)) + 
  geom_point() + 
  scale_x_continuous(breaks = seq(0, 100, 10)) +
  scale_y_continuous(breaks = seq(0, 100, 10)) +
  theme(plot.title = element_text(hjust = 0.5)) +
  labs(colour = "State", x = "x", y = "y", title="B") +
  theme(legend.position="bottom")

#print(gg_xy_coordinates_state_step_99)

#####
# Output generation
#####
# Arrange the two plots in a grid.
gg_grid <- grid.arrange(gg_xy_coordinates_state_step_0, gg_xy_coordinates_state_step_99, ncol=2)

# Print the grid.
print(gg_grid)

# Save the grid.
ggsave(paste0(output_directory, output_filename), dpi = 300, width = 25, height = 15, units = "cm", gg_grid)