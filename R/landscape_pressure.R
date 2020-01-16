#################################################
# Get the directions of all supporters for each #
# step in one run. The directions are constant  #
# for all random seeds.                         #
#################################################

#####
# Initialization
#####
# Clear everything.
rm(list=ls())

# Load packages.
require("ggplot2")
require("data.table")
require("gdata")
require("gridExtra")

# Set the working directory.
setwd("C:/Users/moritz/Google\ Drive/RUB/Masterarbeit/MATISSE\ workspace/Analysis")
# Set the number of threads. Should match the number of logical CPUs.
setDTthreads(4)

# Set the files to be analyzed.
input_baseline <- "../matisse/output/Single\ runs/supporters_output_baseline.csv"
input_policy <- "../matisse/output/Single\ runs/supporters_output_policy.csv"
# Set the output directory.
output_directory <- "../../Latex/pictures/"
# Set the names of the plots to be produced.
output_filename_baseline <- "landscape_pressure_baseline.png"
output_filename_policy <- "landscape_pressure_policy.png"

# Read the file.
supporters_output_baseline <- fread(input_baseline, header=TRUE)
supporters_output_policy <- fread(input_policy, header=TRUE)

# Drop the last column, which is empty.
supporters_output_baseline[,dim(supporters_output_baseline)[2]:=NULL]
supporters_output_policy[,dim(supporters_output_policy)[2]:=NULL]

#####
# Data operations
#####
# Drop everything but the years and the directions.
directions_baseline <- supporters_output_baseline[, print(.SD), .SDcols = grepl("year|direction", names(supporters_output_baseline))]
directions_policy <- supporters_output_policy[, print(.SD), .SDcols = grepl("year|direction", names(supporters_output_policy))]
# Drop the burn-in pahse
directions_baseline <- directions_baseline[year >= 20]
directions_policy <- directions_policy[year >= 20]

# Calculate the means for all supporters.
direction_co2_mean_baseline <- directions_baseline[ , .(Emissions = apply(.SD, 1, mean)), .SDcols = grep("direction_co2_", names(directions_baseline))]
direction_cost_mean_baseline <- directions_baseline[ , .(Cost = apply(.SD, 1, mean)), .SDcols = grep("direction_cost_", names(directions_baseline))]
direction_mit_mean_baseline <- directions_baseline[ , .(MIT = apply(.SD, 1, mean)), .SDcols = grep("direction_mit_", names(directions_baseline))]
direction_be_mean_baseline <- directions_baseline[ , .(BE = apply(.SD, 1, mean)), .SDcols = grep("direction_be_", names(directions_baseline))]
direction_pt_mean_baseline <- directions_baseline[ , .(PT = apply(.SD, 1, mean)), .SDcols = grep("direction_pt_", names(directions_baseline))]
direction_conv_mean_baseline <- directions_baseline[ , .(Conv = apply(.SD, 1, mean)), .SDcols = grep("direction_conv_", names(directions_baseline))]
# Calculate the means for all supporters.
direction_co2_mean_policy <- directions_policy[ , .(Emissions = apply(.SD, 1, mean)), .SDcols = grep("direction_co2_", names(directions_policy))]
direction_cost_mean_policy <- directions_policy[ , .(Cost = apply(.SD, 1, mean)), .SDcols = grep("direction_cost_", names(directions_policy))]
direction_mit_mean_policy <- directions_policy[ , .(MIT = apply(.SD, 1, mean)), .SDcols = grep("direction_mit_", names(directions_policy))]
direction_be_mean_policy <- directions_policy[ , .(BE = apply(.SD, 1, mean)), .SDcols = grep("direction_be_", names(directions_policy))]
direction_pt_mean_policy <- directions_policy[ , .(PT = apply(.SD, 1, mean)), .SDcols = grep("direction_pt_", names(directions_policy))]
direction_conv_mean_policy <- directions_policy[ , .(Conv = apply(.SD, 1, mean)), .SDcols = grep("direction_conv_", names(directions_policy))]

# Add everything to one data.table
directions_means_baseline = data.table(directions_baseline[[1]])
setnames(directions_means_baseline, "year")
directions_means_baseline <- cbind (directions_means_baseline, direction_co2_mean_baseline)
directions_means_baseline <- cbind (directions_means_baseline, direction_cost_mean_baseline)
directions_means_baseline <- cbind (directions_means_baseline, direction_mit_mean_baseline)
directions_means_baseline <- cbind (directions_means_baseline, direction_be_mean_baseline)
directions_means_baseline <- cbind (directions_means_baseline, direction_pt_mean_baseline)
directions_means_baseline <- cbind (directions_means_baseline, direction_conv_mean_baseline)
# Add everything to one data.table
directions_means_policy = data.table(directions_policy[[1]])
setnames(directions_means_policy, "year")
directions_means_policy <- cbind (directions_means_policy, direction_co2_mean_policy)
directions_means_policy <- cbind (directions_means_policy, direction_cost_mean_policy)
directions_means_policy <- cbind (directions_means_policy, direction_mit_mean_policy)
directions_means_policy <- cbind (directions_means_policy, direction_be_mean_policy)
directions_means_policy <- cbind (directions_means_policy, direction_pt_mean_policy)
directions_means_policy <- cbind (directions_means_policy, direction_conv_mean_policy)

# Create cummulative values
directions_means_baseline_cummulative <- directions_means_baseline[, cumsum(.SD), .SDcols = !(grepl("year", names(directions_means_baseline)))]
directions_means_baseline_cummulative <- cbind(directions_means_baseline[["year"]], directions_means_baseline_cummulative)
setnames(directions_means_baseline_cummulative, "V1", "year")

directions_means_policy_cummulative <- directions_means_policy[, cumsum(.SD), .SDcols = !(grepl("year", names(directions_means_policy)))]
directions_means_policy_cummulative <- cbind(directions_means_policy[["year"]], directions_means_policy_cummulative)
setnames(directions_means_policy_cummulative, "V1", "year")

#####
# Output generation
#####
# Melt it to plot it
directions_means_baseline_melted <- melt(directions_means_baseline, id="year")
directions_means_baseline_cummulative_melted <- melt(directions_means_baseline_cummulative, id="year")
directions_means_policy_melted <- melt(directions_means_policy, id="year")
directions_means_policy_cummulative_melted <- melt(directions_means_policy_cummulative, id="year")

# Plot it
gg_directions_means_baseline <- ggplot(directions_means_baseline_melted, aes(x=year, y=value, colour=variable)) +
  labs(x = "Time", y = "Value", colour="Direction", title="A") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position="bottom") +
  geom_line()
print(gg_directions_means_baseline)
gg_directions_means_baseline_cummulative <- ggplot(directions_means_baseline_cummulative_melted, aes(x=year, y=value, colour=variable)) +
  labs(x = "Time", y = "Value", colour="Direction", title="B") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position="none") +
  geom_line()
print(gg_directions_means_baseline_cummulative)

baseline_output <- grid.arrange(gg_directions_means_baseline, gg_directions_means_baseline_cummulative, ncol=2)

gg_directions_means_policy <- ggplot(directions_means_policy_melted, aes(x=year, y=value, colour=variable)) +
  labs(x = "Time", y = "Value", colour="Direction", title="A") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position="bottom") +
  geom_line()
print(gg_directions_means_policy)
gg_directions_means_policy_cummulative <- ggplot(directions_means_policy_cummulative_melted, aes(x=year, y=value, colour=variable)) +
  labs(x = "Time", y = "Value", colour="Direction", title="B") +
  theme(plot.title = element_text(hjust = 0.5)) +
  scale_y_continuous(breaks=seq(-25, 75, 25), limits = c(-25, 75)) +
  theme(legend.position="none") +
  geom_line()
print(gg_directions_means_policy_cummulative)

policy_output <- grid.arrange(gg_directions_means_policy, gg_directions_means_policy_cummulative, ncol=2)

# Save it
ggsave(paste0(output_directory, output_filename_baseline), dpi = 300, width = 20, height = 10, units = "cm", baseline_output)
ggsave(paste0(output_directory, output_filename_policy), dpi = 300, width = 20, height = 10, units = "cm", policy_output)
