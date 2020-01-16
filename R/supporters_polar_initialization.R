#################################################
# Print a bar plot of the initial values of     #
# the supporters.                               #
#################################################

# Clear everything
rm(list=ls())

# Load packages
require("ggplot2")
require("data.table")

# Set working directory
setwd("C:/Users/moritz/Google\ Drive/RUB/Masterarbeit/MATISSE\ workspace/matisse/output/batch_output")

################################################
# Initialization
################################################
directory <- "../../../../Latex/pictures/"

# 0: baseline
# 1: policy
switch <- 0

if(switch == 0) {
  case <- "baseline"
} else {
  case <- "policy"
}

# Set the file name
filename_supporters_output <- paste0("supporters_output_batch_", case, ".csv")
# Read the files
supporters_output <- fread(filename_supporters_output, header=TRUE, na.strings="null")

# Drop last column, because it is empty
supporters_output[,dim(supporters_output)[2]:=NULL]
# Raise the year counter by one (bug in the output generating process)
set(supporters_output, i=NULL, j=1L, value=supporters_output[[1]]+1L)

################################################
# Plot the average practices in the first step
################################################
# Get the practices in the first step
practices_initialization <- supporters_output[year == 2, print(.SD), .SDcols = grepl("year|practice", names(supporters_output))]
# Calculate the means for each supporter for all runs
practices_initialization_means <- practices_initialization[, lapply(.SD, mean), by=year, .SDcols = names(practices_initialization)]
# Calculate the means for all supporters
practice_co2_mean <- practices_initialization_means[ , .(CO2 = apply(.SD, 1, mean)), .SDcols = grep("practice_co2_", names(practices_initialization_means))]
practice_cost_mean <- practices_initialization_means[ , .(Cost = apply(.SD, 1, mean)), .SDcols = grep("practice_cost_", names(practices_initialization_means))]
practice_mit_mean <- practices_initialization_means[ , .(MIT = apply(.SD, 1, mean)), .SDcols = grep("practice_mit_", names(practices_initialization_means))]
practice_be_mean <- practices_initialization_means[ , .(BE = apply(.SD, 1, mean)), .SDcols = grep("practice_be_", names(practices_initialization_means))]
practice_pt_mean <- practices_initialization_means[ , .(PT = apply(.SD, 1, mean)), .SDcols = grep("practice_pt_", names(practices_initialization_means))]
practice_conv_mean <- practices_initialization_means[ , .(Conv = apply(.SD, 1, mean)), .SDcols = grep("practice_conv_", names(practices_initialization_means))]

# Add everything to one data.table
practices_initialization = data.table()
practices_initialization <- cbind (practices_initialization, practice_co2_mean)
practices_initialization <- cbind (practices_initialization, practice_cost_mean)
practices_initialization <- cbind (practices_initialization, practice_mit_mean)
practices_initialization <- cbind (practices_initialization, practice_be_mean)
practices_initialization <- cbind (practices_initialization, practice_pt_mean)
practices_initialization <- cbind (practices_initialization, practice_conv_mean)

# Melt it to plot it
practices_initialization_melted <- melt(practices_initialization)

# Plot it
gg_practices_initialization_melted <- ggplot(practices_initialization_melted, aes(x=variable, y=value)) +
  geom_bar(stat="identity") +
  labs(x = "Practice", y = "Value") +
  scale_y_continuous(breaks=seq(0, 100, 10))
print(gg_practices_initialization_melted) 
# Save it
filename_to_be_saved <- paste0(directory, "supporters_initialization_", case, ".png")
ggsave(filename_to_be_saved, dpi = 300, width = 15, height = 15, units = "cm", gg_practices_initialization_melted)