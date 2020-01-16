#################################################
# Get the values of the technologies in the     #
# practice space in one run and plot them in    #
# polar coordinate system.                      #
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
input <- "../matisse/output/technologies_output.csv"
# Set the output directory.
output_directory <- "../../Latex/pictures/"
# Set the name of the plot to be produced.
output_filename <- "technologies_polar_initialization.png"

# Read the file.
technologies_output <- fread(input, header=TRUE, na.strings="null")

# Drop last column, because it is empty.
technologies_output[,dim(technologies_output)[2]:=NULL]

#####
# Data operations
#####
# Drop everything but the names and the practices.
practices_step_0 <- technologies_output[year==1, print(.SD), .SDcols = !grepl("direction|state|number_supporters|strength|year", names(technologies_output))]

# Split up the df every 7th step and store it in a new data.table.
technology_characteristics = data.table()

for (i in seq(1, dim(practices_step_0)[2], 7)) {
  tmp <-  practices_step_0[, seq(from=i, to=i+6), with=FALSE]
  setnames(tmp, colnames(tmp)[1], "Name")
  setnames(tmp, colnames(tmp)[2], "Emissions")
  setnames(tmp, colnames(tmp)[3], "Cost")
  setnames(tmp, colnames(tmp)[4], "MIT")
  setnames(tmp, colnames(tmp)[5], "BE")
  setnames(tmp, colnames(tmp)[6], "PT")
  setnames(tmp, colnames(tmp)[7], "Conv")
  technology_characteristics <- rbind(technology_characteristics, tmp)
}
# Remove non-available entries, which can exist due to clustering/absorption.
technology_characteristics <- na.omit(technology_characteristics, "Name")
# Melt to plot it.
technology_characteristics <- melt(technology_characteristics, id="Name")
# Plot it.
gg_technology_characteristics <- ggplot(technology_characteristics, aes(x=variable, y=value, fill=variable)) + 
  geom_col() + 
  theme_bw() + 
  coord_polar() + 
  labs(colour = "Technology", x = "", y = "", fill = "Practice") +
  facet_wrap(~Name, ncol = 2) +
  theme(legend.position = "bottom")

print(gg_technology_characteristics)

#####
# Output generation
#####
# Save it.
ggsave(paste0(output_directory, output_filename), dpi = 300, width = 20, height = 30, units = "cm")
