#################################################
# Get the values of the supporters in the       #
# practice space in one run and plot them in    #
# two three-dimensional grids.                  #
#################################################

#####
# Initialization
#####
# Clear everything.
rm(list=ls())

# Load packages.
require("ggplot2")
require("data.table")
require("plot3D")
require("gridExtra")

# Set the working directory.
setwd("C:/Users/moritz/Google\ Drive/RUB/Masterarbeit/MATISSE\ workspace/Analysis")
# Set the number of threads. Should match the number of logical CPUs.
setDTthreads(4)

# Set the file to be analyzed.
input <- "../matisse/output/supporters_output.csv"
# Set the output directory.
output_directory <- "../../Latex/pictures/"
# Set the name of the plot to be produced.
output_filename <- "3d_initial_supporter_distribution.png"

# Read the file.
supporters_output <- fread(input, header=TRUE)

# Drop the last column, which is empty.
supporters_output[,dim(supporters_output)[2]:=NULL]

#####
# Data operations
#####
# Get the chosen technology in the first step.
chosen_technology_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("chosen_technology_", names(supporters_output))]
chosen_technology_step_0 <- transpose(chosen_technology_step_0)
chosen_technology_step_0 <- as.list(chosen_technology_step_0)

# Get the first dimension in the first step.
practice_co2_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_co2_", names(supporters_output))]
practice_co2_step_0 <- transpose(practice_co2_step_0)
practice_co2_step_0 <- practice_co2_step_0[['V1']]

# Get the second dimension in the first step.
practice_cost_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_cost_", names(supporters_output))]
practice_cost_step_0 <- transpose(practice_cost_step_0)
practice_cost_step_0 <- practice_cost_step_0[['V1']]

# Get the third dimension in the first step.
practice_mit_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_mit_", names(supporters_output))]
practice_mit_step_0 <- transpose(practice_mit_step_0)
practice_mit_step_0 <- practice_mit_step_0[['V1']]

# Get the fourth dimension in the first step.
practice_be_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_be_", names(supporters_output))]
practice_be_step_0 <- transpose(practice_be_step_0)
practice_be_step_0 <- practice_be_step_0[['V1']]

# Get the fifth dimension in the first step.
practice_pt_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_pt_", names(supporters_output))]
practice_pt_step_0 <- transpose(practice_pt_step_0)
practice_pt_step_0 <- practice_pt_step_0[['V1']]

# Get the sixth dimension in the first step.
practice_conv_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_conv_", names(supporters_output))]
practice_conv_step_0 <- transpose(practice_conv_step_0)
practice_conv_step_0 <- practice_conv_step_0[['V1']]

#####
# Output generation
#####
# Set the ppi.
ppi <- 300
# Set output details.
png(paste0(output_directory, output_filename), width=12*ppi, height=6*ppi, res=ppi)

# Arrange the plots.
par(mfrow=c(1,2))

# Produce the plots.
scatter3D(x = practice_co2_step_0, y = practice_cost_step_0, z = practice_mit_step_0, bty = "b2",  phi = 25, theta = 35,
          xlab = "Emission", ylab ="Cost", zlab = "MIT", ticktype = "detailed", clab = "MIT", resfac = 10, 
          xlim = c(0, 100), ylim = c(0, 100), zlim = c(0, 100))
scatter3D(x = practice_be_step_0, y = practice_pt_step_0, z = practice_conv_step_0, bty = "b2",  phi = 25, theta = 35,
          xlab = "BE", ylab ="PT", zlab = "Conv", ticktype = "detailed", clab = "Conv", resfac = 10,
          xlim = c(0, 100), ylim = c(0, 100), zlim = c(0, 100))

# Save the plots.
dev.off()