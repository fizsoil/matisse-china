###################################################
# Get the values of the technologies and of       #
# the supporters of the Emissions and the MIT     #
# in one run and plot them in two separate plots. #
###################################################

#####
# Initialization
#####
# Clear everything.
rm(list=ls())

# Load packages.
require("ggplot2")
require("data.table")
require("gridExtra")

# Set the working directory.
setwd("C:/Users/moritz/Google\ Drive/RUB/Masterarbeit/MATISSE\ workspace/Analysis")
# Set the number of threads. Should match the number of logical CPUs.
setDTthreads(4)

# Set the files to be analyzed.
input_supporters <- "../matisse/output/supporters_output.csv"
input_technologies <- "../matisse/output/technologies_output.csv"
# Set the output directory.
output_directory <- "../../Latex/pictures/"
# Set the name of the plot to be produced.
output_filename <- "practice_space_two_dimensions_step_0_plot.png"

# Read the files.
supporters_output <- fread(input_supporters, header=TRUE)
technologies_output <- fread(input_technologies, header=TRUE, na.strings="null")

# Drop the last columns, which are empty.
supporters_output[,dim(supporters_output)[2]:=NULL]
technologies_output[,dim(technologies_output)[2]:=NULL]

#####
# Data operations
#####

#### Get the practices of the technologies. ####

# Drop everything but the names, the practices and the directions.
practices_step_0 <- technologies_output[year==1, print(.SD), .SDcols = grepl("name|practice", names(technologies_output))]
# Drop the directions.
practices_step_0 <- practices_step_0[, print(.SD), .SDcols = !grep("direction", names(practices_step_0))]

# Split up the df every 7th step and store it in a new data.table.
technologies_step_0 = data.table()
for (i in seq(1, dim(practices_step_0)[2], 7)) {
  tmp <-  practices_step_0[, seq(from=i, to=i+6), with=FALSE]
  setnames(tmp, colnames(tmp)[1], "name")
  setnames(tmp, colnames(tmp)[2], "co2")
  setnames(tmp, colnames(tmp)[3], "cost")
  setnames(tmp, colnames(tmp)[4], "MIT")
  setnames(tmp, colnames(tmp)[5], "BE")
  setnames(tmp, colnames(tmp)[6], "PT")
  setnames(tmp, colnames(tmp)[7], "Conv")
  technologies_step_0 <- rbind(technologies_step_0, tmp)
}
# Remove non-available entries, which can exist due to clustering/absorption.
technologies_step_0 <- na.omit(technologies_step_0, "name")

# Plot it.
gg_technologies_step_0 <- ggplot(technologies_step_0, aes(x=co2, y=MIT, colour = name)) +
  theme(legend.position="right") +
  labs(x = "Emissions", y = "MIT", colour = "Technology", title="A") +
  theme(plot.title = element_text(hjust = 0.5)) +
  scale_y_continuous(breaks=seq(0, 100, 25), limits = c(0, 100)) +
  scale_x_continuous(breaks=seq(0, 100, 25), limits = c(0, 100)) +
  geom_point()

print(gg_technologies_step_0)

#### Get the practices of the supporters. ####

# Get the chosen technology in the first step.
chosen_technology_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("chosen_technology_", names(supporters_output))]
chosen_technology_step_0 <- transpose(chosen_technology_step_0)
setnames(chosen_technology_step_0, "chosen_technology_0")

# Get the Emissions in the first step.
practice_co2_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_co2_", names(supporters_output))]
practice_co2_step_0 <- transpose(practice_co2_step_0)
setnames(practice_co2_step_0, "practice_co2_step_0")

# Get the MIT in the first step.
practice_mit_step_0 <- supporters_output[year==1, print(.SD), .SDcols = grep("practice_mit_", names(supporters_output))]
practice_mit_step_0 <- transpose(practice_mit_step_0)
setnames(practice_mit_step_0, "practice_mit_step_0")

# Bind it together.
practice_space_co2_mit_step_0 <- cbind(chosen_technology_step_0, practice_co2_step_0, practice_mit_step_0)

# Plot it.
gg_practice_space_co2_mit_step_0 <- ggplot(practice_space_co2_mit_step_0, 
  aes(x=practice_co2_step_0, y=practice_mit_step_0, colour = chosen_technology_0)) +
  labs(x = "Emissions", y = "MIT", colour = "Chosen technology", title="B") + 
  theme(legend.position="none") +
  theme(plot.title = element_text(hjust = 0.5)) +
  scale_y_continuous(breaks=seq(0, 100, 25), limits = c(0, 100)) +
  scale_x_continuous(breaks=seq(0, 100, 25), limits = c(0, 100)) +
  geom_point()

print(gg_practice_space_co2_mit_step_0)

output <- grid.arrange(gg_technologies_step_0, gg_practice_space_co2_mit_step_0, ncol=2)

ggsave(paste0(output_directory, output_filename), dpi = 300, width = 20, height = 10, units = "cm", output)
