#################################################
# Compare the baseline to the policy scenario   #
#################################################

# Clear everything
rm(list=ls())

# Load packages
require("ggplot2")
require("data.table")
require("gridExtra")
require("Hmisc")
require("gdata")

# Set working directory
setwd("C:/Users/moritz/Google\ Drive/RUB/Masterarbeit/MATISSE\ workspace/matisse/output/batch_output")


################################################
# Initialization
################################################
# Set the ouput directory
output_directory <- "../../../../Latex/pictures/"

################################################
# Baseline                                     #
################################################
case <- "baseline"

# Set the file names
filename_technologies_output <- paste0("technologies_output_batch_", case, ".csv")
filename_summary_output <- paste0("summary_output_batch_", case, ".csv")

# Read the files
technologies_output <- fread(filename_technologies_output, header=TRUE, na.strings="null")
summary_output <- fread(filename_summary_output, header=TRUE, na.string="null")

# Drop last column, because it is empty
technologies_output[,dim(technologies_output)[2]:=NULL]

# Function to replace NA values with 0. NA values exist, because those technologies were absorbed/clustered with.
f_replace_na = function(DT) {
  for (j in names(DT))
    set(DT, which(is.na(DT[[j]])), j, 0)
}


## Plot the means of the normalized strengths

# Replace the NA values
f_replace_na(summary_output)
# Calculate the mean in each year for every column
normalized_strength_means <- summary_output[, lapply(.SD, mean), by=year, .SDcols = names(summary_output)]
# Drop the first column, because it is a duplicate
normalized_strength_means[, 1:=NULL]
# Drop the first twenty rows, because they are the burn-in phase
normalized_strength_means <- normalized_strength_means[year >= 20]
# Melt it to plot it
normalized_strength_means_melted <- melt(normalized_strength_means, id="year")
gg_normalized_strength_means_plot <- ggplot(normalized_strength_means_melted, aes(x=year, y = value, colour = variable)) + 
  geom_line() +
  labs(colour = "Technology", x = "Time", y = "Strength", title="A") +
  theme(legend.position = "bottom") +
  theme(plot.title = element_text(hjust = 0.5)) +
  scale_y_continuous(breaks=seq(0, 1, 0.1), limits = c(0, 0.5)) +
  scale_x_continuous(breaks=seq(0, 100, 10))
#print(gg_normalized_strength_means_plot)


## Plot boxplots of the final normalized strengths
## for all runs.

# Drop the first 20 rows of each run, because they are the burn-in phase
summary_output_final_values <- summary_output[year == 100]
# Enumerate the runs
set(summary_output_final_values, i=NULL, j=1L, value=seq(from=1, to=dim(summary_output_final_values)[1], by=1))
# Rename the "year" column
setnames(summary_output_final_values, "year", "run")
# Melt it to plot it
summary_output_final_values_melted <- melt(summary_output_final_values, id="run")
# Plot it
gg_summary_output_final_values_boxplot <- ggplot(summary_output_final_values_melted, aes(x=variable, y=value, color=variable)) +
  geom_boxplot() +
  labs(colour = "Technology", x = "Technology", y = "Strength", title="B") +
  theme(legend.position = "none") +
  theme(plot.title = element_text(hjust = 0.5)) +
  scale_y_continuous(breaks=seq(0, 1, 0.1), limits = c(0, 0.6))
#print(gg_summary_output_final_values_boxplot)


## Plot practiced practices.

# Drop NA values
f_replace_na(technologies_output)
# Calculate the mean in each year for every column except for those containing the names and the states (Regime, EN, Niche)
means_per_year <- technologies_output[, lapply(.SD, mean), by=year, .SDcols = !grepl("name|state", names(technologies_output))]
# Drop the first column, because it is a duplicate
means_per_year[, 1:=NULL]
# Drop the first twenty rows, because they are the burn-in phase
means_per_year <- means_per_year[year >= 20]

# Drop direction and strength columns 
practices_means <- means_per_year[, print(.SD), .SDcols = !grepl("direction|strength", names(means_per_year))]

# Get the columns with the number of supporters
index_technology <- grep("number_supporters", names(practices_means))

# Loop through all the columns with the number of supporters
for (k in index_technology) {
  # Get the name of the current technology
  name_of_technology <- sub("number_supporters_", "", names(practices_means)[k])
  # Get the columns of all practices of this technology
  index_practices <- grep(name_of_technology, names(practices_means))
  # Drop the column for number of supporters
  index_practices <- index_practices[-(1)]
  # Loop through all the practices and multiply them with the
  # number of supporters
  for(l in index_practices) {
    set(practices_means, i=NULL, j=l, value=practices_means[[k]]*practices_means[[l]])
  }
}

# Sum up all the rows
practices_means[, pp_CO2 := rowSums(.SD), .SDcols = grep("co2", names(practices_means))]
practices_means[, pp_Cost := rowSums(.SD), .SDcols = grep("cost", names(practices_means))]
practices_means[, pp_MIT := rowSums(.SD), .SDcols = grep("mit", names(practices_means))]
practices_means[, pp_BE := rowSums(.SD), .SDcols = grep("be", names(practices_means))]
practices_means[, pp_PT := rowSums(.SD), .SDcols = grep("pt", names(practices_means))]
practices_means[, pp_Conv := rowSums(.SD), .SDcols = grep("conv", names(practices_means))]

# Extract the year and the practices
practiced_practices_means <- practices_means[, print(.SD), .SDcols = grepl("year|pp_", names(practices_means))]

# Rename columns
setnames(practiced_practices_means, sub("pp_","", names(practiced_practices_means)))

# Store MIT, PT and Emissions for comparison
practiced_practices_mit_pt_co2 <- practiced_practices_means[, print(.SD), .SDcols = grepl("CO2|MIT|PT|year", names(practiced_practices_means))]
practiced_practices_mit_pt_co2[, Scenario:=capitalize(case)]
assign(paste0("practiced_practices_mit_pt_co2_", case), practiced_practices_mit_pt_co2)

# Melt it to plot it
practiced_practices_melted <- melt(practiced_practices_means, id="year")
# Plot it
gg_practiced_practices_plot <- ggplot(practiced_practices_melted, aes(x=year, y=value, color=variable)) + 
  geom_line() +
  labs(colour = "Practices", x = "Time", y = "Value", title="C") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "bottom") +
  scale_y_continuous(breaks=seq(0, 100, 10), limits = c(0, 100)) +
  scale_x_continuous(breaks=seq(0, 100, 10))
#print(gg_practiced_practices_plot)


## Plot boxplots of practiced practices
practices <- technologies_output[year == 100, print(.SD), .SDcols = !grepl("direction|strength|name|state", names(technologies_output))]

# Get the columns with the number of supporters
index_technology <- grep("number_supporters", names(practices))
#practiced_practices = data.table()
# Loop through all the columns with the number of supporters
for (k in index_technology) {
  # Get the name of the current technology
  name_of_technology <- sub("number_supporters_", "", names(practices)[k])
  # Get the columns of all practices of this technology
  index_practices <- grep(name_of_technology, names(practices))
  # Drop the column for number of supporters
  index_practices <- index_practices[-(1)]
  # Loop through all the practices and multiply them with the
  # number of supporters
  for(l in index_practices) {
    set(practices, i=NULL, j=l, value=practices[[k]]*practices[[l]])
  }
}

practices[, pp_CO2 := rowSums(.SD), .SDcols = grep("co2", names(practices))]
practices[, pp_Cost := rowSums(.SD), .SDcols = grep("cost", names(practices))]
practices[, pp_MIT := rowSums(.SD), .SDcols = grep("mit", names(practices))]
practices[, pp_BE := rowSums(.SD), .SDcols = grep("be", names(practices))]
practices[, pp_PT := rowSums(.SD), .SDcols = grep("pt", names(practices))]
practices[, pp_Conv := rowSums(.SD), .SDcols = grep("conv", names(practices))]

# Extract the year and the practices
practiced_practices <- practices[, print(.SD), .SDcols = grepl("year|pp_", names(practices))]

# Rename columns
setnames(practiced_practices, sub("pp_","", names(practiced_practices)))

# Melt it to plot it
practiced_practices_melted <- melt(practiced_practices, id="year")

gg_practiced_practices_boxplot <- ggplot(practiced_practices_melted, aes(x=variable, y=value, color=variable)) + 
  geom_boxplot() +
  labs(colour = "Practices", x = "Practiced practice", y = "Value", title="D") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "none") +
  scale_y_continuous(breaks=seq(0, 100, 10), limits = c(0, 100))
#print(gg_practiced_practices_boxplot)

# Arrange the plots
output_plot <- grid.arrange(gg_normalized_strength_means_plot, gg_summary_output_final_values_boxplot, gg_practiced_practices_plot, gg_practiced_practices_boxplot,  ncol=2)

# Paste the filname together
filename_to_be_saved <- paste0(output_directory, "summary_plot_", case, ".png")
# Save it
ggsave(filename_to_be_saved, dpi = 600, width = 30, height = 20, units = "cm", output_plot)

## Plot change of emissions
# Effective emissions: Emissions * Usage

effective_emissions <- practiced_practices_means
set(effective_emissions, i=NULL, j=2L, value=effective_emissions[['CO2']]*(effective_emissions[['MIT']]+effective_emissions[['PT']]))
effective_emissions <- effective_emissions[, print(.SD), .SDcols = grepl("year|CO2", names(effective_emissions))]

setnames(effective_emissions, "CO2", "EE")

assign(paste0("effective_emissions_", case), effective_emissions)

gg_effective_emissions <- ggplot(effective_emissions, aes(x=year, y=EE)) + 
  geom_line() +
  labs(colour = "Effective emissions", x = "Time", y = "Effective emissions", title=capitalize(case)) +
  theme(legend.position = "none") +
  #scale_y_continuous(breaks=seq(3500, 7500, 500), limits = c(3500, 7500)) +
  scale_x_continuous(breaks=seq(0, 100, 10))
print(gg_effective_emissions)

# Paste the filename together
filename_to_be_saved <- paste0(output_directory, "effective_emissions_", case, ".png")
# Save it
ggsave(filename_to_be_saved, dpi = 300, width = 10, height = 10, units = "cm", gg_effective_emissions)


################################################
# Policy                                       #
################################################
case <- "policy"

# Set the file names
filename_technologies_output <- paste0("technologies_output_batch_", case, ".csv")
filename_summary_output <- paste0("summary_output_batch_", case, ".csv")

# Read the files
technologies_output <- fread(filename_technologies_output, header=TRUE, na.strings="null")
summary_output <- fread(filename_summary_output, header=TRUE, na.string="null")

# Drop last column, because it is empty
technologies_output[,dim(technologies_output)[2]:=NULL]

# Function to replace NA values with 0. NA values exist, because those technologies were absorbed/clustered with.
f_replace_na = function(DT) {
  for (j in names(DT))
    set(DT, which(is.na(DT[[j]])), j, 0)
}


## Plot the means of the normalized strengths

# Replace the NA values
f_replace_na(summary_output)
# Calculate the mean in each year for every column
normalized_strength_means <- summary_output[, lapply(.SD, mean), by=year, .SDcols = names(summary_output)]
# Drop the first column, because it is a duplicate
normalized_strength_means[, 1:=NULL]
# Drop the first twenty rows, because they are the burn-in phase
normalized_strength_means <- normalized_strength_means[year >= 20]
# Melt it to plot it
normalized_strength_means_melted <- melt(normalized_strength_means, id="year")
gg_normalized_strength_means_plot <- ggplot(normalized_strength_means_melted, aes(x=year, y = value, colour = variable)) + 
  geom_line() +
  labs(colour = "Technology", x = "Time", y = "Strength", title="A") +
  theme(legend.position = "bottom") +
  theme(plot.title = element_text(hjust = 0.5)) +
  scale_y_continuous(breaks=seq(0, 1, 0.1), limits = c(0, 0.5)) +
  scale_x_continuous(breaks=seq(0, 100, 10))
#print(gg_normalized_strength_means_plot)


## Plot boxplots of the final normalized strengths
## for all runs.

# Drop the first 20 rows of each run, because they are the burn-in phase
summary_output_final_values <- summary_output[year == 100]
# Enumerate the runs
set(summary_output_final_values, i=NULL, j=1L, value=seq(from=1, to=dim(summary_output_final_values)[1], by=1))
# Rename the "year" column
setnames(summary_output_final_values, "year", "run")
# Melt it to plot it
summary_output_final_values_melted <- melt(summary_output_final_values, id="run")
# Plot it
gg_summary_output_final_values_boxplot <- ggplot(summary_output_final_values_melted, aes(x=variable, y=value, color=variable)) +
  geom_boxplot() +
  labs(colour = "Technology", x = "Technology", y = "Strength", title="B") +
  theme(legend.position = "none") +
  theme(plot.title = element_text(hjust = 0.5)) +
  scale_y_continuous(breaks=seq(0, 1, 0.1), limits = c(0, 0.6))
#print(gg_summary_output_final_values_boxplot)


## Plot practiced practices.

# Drop NA values
f_replace_na(technologies_output)
# Calculate the mean in each year for every column except for those containing the names and the states (Regime, EN, Niche)
means_per_year <- technologies_output[, lapply(.SD, mean), by=year, .SDcols = !grepl("name|state", names(technologies_output))]
# Drop the first column, because it is a duplicate
means_per_year[, 1:=NULL]
# Drop the first twenty rows, because they are the burn-in phase
means_per_year <- means_per_year[year >= 20]

# Drop direction and strength columns 
practices_means <- means_per_year[, print(.SD), .SDcols = !grepl("direction|strength", names(means_per_year))]

# Get the columns with the number of supporters
index_technology <- grep("number_supporters", names(practices_means))

# Loop through all the columns with the number of supporters
for (k in index_technology) {
  # Get the name of the current technology
  name_of_technology <- sub("number_supporters_", "", names(practices_means)[k])
  # Get the columns of all practices of this technology
  index_practices <- grep(name_of_technology, names(practices_means))
  # Drop the column for number of supporters
  index_practices <- index_practices[-(1)]
  # Loop through all the practices and multiply them with the
  # number of supporters
  for(l in index_practices) {
    set(practices_means, i=NULL, j=l, value=practices_means[[k]]*practices_means[[l]])
  }
}

# Sum up all the rows
practices_means[, pp_CO2 := rowSums(.SD), .SDcols = grep("co2", names(practices_means))]
practices_means[, pp_Cost := rowSums(.SD), .SDcols = grep("cost", names(practices_means))]
practices_means[, pp_MIT := rowSums(.SD), .SDcols = grep("mit", names(practices_means))]
practices_means[, pp_BE := rowSums(.SD), .SDcols = grep("be", names(practices_means))]
practices_means[, pp_PT := rowSums(.SD), .SDcols = grep("pt", names(practices_means))]
practices_means[, pp_Conv := rowSums(.SD), .SDcols = grep("conv", names(practices_means))]

# Extract the year and the practices
practiced_practices_means <- practices_means[, print(.SD), .SDcols = grepl("year|pp_", names(practices_means))]

# Rename columns
setnames(practiced_practices_means, sub("pp_","", names(practiced_practices_means)))

# Store MIT, PT and Emissions for comparison
practiced_practices_mit_pt_co2 <- practiced_practices_means[, print(.SD), .SDcols = grepl("CO2|MIT|PT|year", names(practiced_practices_means))]
practiced_practices_mit_pt_co2[, Scenario:=capitalize(case)]
assign(paste0("practiced_practices_mit_pt_co2_", case), practiced_practices_mit_pt_co2)

# Melt it to plot it
practiced_practices_melted <- melt(practiced_practices_means, id="year")
# Plot it
gg_practiced_practices_plot <- ggplot(practiced_practices_melted, aes(x=year, y=value, color=variable)) + 
  geom_line() +
  labs(colour = "Practices", x = "Time", y = "Value", title="C") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "bottom") +
  scale_y_continuous(breaks=seq(0, 100, 10), limits = c(0, 100)) +
  scale_x_continuous(breaks=seq(0, 100, 10))
#print(gg_practiced_practices_plot)


## Plot boxplots of practiced practices
practices <- technologies_output[year == 100, print(.SD), .SDcols = !grepl("direction|strength|name|state", names(technologies_output))]

# Get the columns with the number of supporters
index_technology <- grep("number_supporters", names(practices))
#practiced_practices = data.table()
# Loop through all the columns with the number of supporters
for (k in index_technology) {
  # Get the name of the current technology
  name_of_technology <- sub("number_supporters_", "", names(practices)[k])
  # Get the columns of all practices of this technology
  index_practices <- grep(name_of_technology, names(practices))
  # Drop the column for number of supporters
  index_practices <- index_practices[-(1)]
  # Loop through all the practices and multiply them with the
  # number of supporters
  for(l in index_practices) {
    set(practices, i=NULL, j=l, value=practices[[k]]*practices[[l]])
  }
}

practices[, pp_CO2 := rowSums(.SD), .SDcols = grep("co2", names(practices))]
practices[, pp_Cost := rowSums(.SD), .SDcols = grep("cost", names(practices))]
practices[, pp_MIT := rowSums(.SD), .SDcols = grep("mit", names(practices))]
practices[, pp_BE := rowSums(.SD), .SDcols = grep("be", names(practices))]
practices[, pp_PT := rowSums(.SD), .SDcols = grep("pt", names(practices))]
practices[, pp_Conv := rowSums(.SD), .SDcols = grep("conv", names(practices))]

# Extract the year and the practices
practiced_practices <- practices[, print(.SD), .SDcols = grepl("year|pp_", names(practices))]

# Rename columns
setnames(practiced_practices, sub("pp_","", names(practiced_practices)))

# Melt it to plot it
practiced_practices_melted <- melt(practiced_practices, id="year")

gg_practiced_practices_boxplot <- ggplot(practiced_practices_melted, aes(x=variable, y=value, color=variable)) + 
  geom_boxplot() +
  labs(colour = "Practices", x = "Practiced practice", y = "Value", title="D") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "none") +
  scale_y_continuous(breaks=seq(0, 100, 10), limits = c(0, 100))
#print(gg_practiced_practices_boxplot)

# Arrange the plots
output_plot <- grid.arrange(gg_normalized_strength_means_plot, gg_summary_output_final_values_boxplot, gg_practiced_practices_plot, gg_practiced_practices_boxplot,  ncol=2)

# Paste the filname together
filename_to_be_saved <- paste0(output_directory, "summary_plot_", case, ".png")
# Save it
ggsave(filename_to_be_saved, dpi = 600, width = 30, height = 20, units = "cm", output_plot)


## Plot change of emissions
# Effective emissions: Emissions * Usage

effective_emissions <- practiced_practices_means
set(effective_emissions, i=NULL, j=2L, value=effective_emissions[['CO2']]*(effective_emissions[['MIT']]+effective_emissions[['PT']]))
effective_emissions <- effective_emissions[, print(.SD), .SDcols = grepl("year|CO2", names(effective_emissions))]

setnames(effective_emissions, "CO2", "EE")

assign(paste0("effective_emissions_", case), effective_emissions)

gg_effective_emissions <- ggplot(effective_emissions, aes(x=year, y=EE)) + 
  geom_line() +
  labs(colour = "Effective emissions", x = "Time", y = "Effective emissions", title=capitalize(case)) +
  theme(legend.position = "none") +
  #scale_y_continuous(breaks=seq(3500, 7500, 500), limits = c(3500, 7500)) +
  scale_x_continuous(breaks=seq(0, 100, 10))
print(gg_effective_emissions)

# Paste the filename together
filename_to_be_saved <- paste0(output_directory, "effective_emissions_", case, ".png")
# Save it
ggsave(filename_to_be_saved, dpi = 300, width = 10, height = 10, units = "cm", gg_effective_emissions)



################################################
# Plot the difference in effective emissions
################################################
if(exists("effective_emissions_policy") && exists("effective_emissions_baseline")) {
  # Keep effective_emissions_policy and effective_emissions_baseline
  #keep(effective_emissions_policy, effective_emissions_baseline, sure = TRUE)
  
  # Prepare the data
  effective_emissions_baseline_tmp <- effective_emissions_baseline[]
  setnames(effective_emissions_baseline_tmp, "EE", "Baseline")
  
  effective_emissions_policy_tmp <- effective_emissions_policy[, "EE"]
  setnames(effective_emissions_policy_tmp, "Policy")
  
  effective_emissions <- cbind(effective_emissions_baseline_tmp, effective_emissions_policy_tmp)
  
  # Melt to plot it
  effective_emissions_melted <- melt(effective_emissions, id="year")
  
  # Plot two boxplots for each effective emissions time series
  gg_effective_emissions_comp <- ggplot(effective_emissions_melted, aes(x=year, y=value, colour=variable)) + 
    geom_line() +
    labs(colour = "Scenario", x = "Time", y = "Effective Emissions", title="B") +
    theme(plot.title = element_text(hjust = 0.5)) +
    theme(legend.position = "bottom") + 
#    scale_y_continuous(breaks=seq(1500, 7500, 250)) +
    scale_x_continuous(breaks=seq(0, 100, 10))
  print(gg_effective_emissions_comp)
  
  # Paste the filename together
  # filename_to_be_saved <- paste0(output_directory, "effective_emissions_comp.png")
  # Save it
  # ggsave(filename_to_be_saved, dpi = 600, width = 15, height = 15, units = "cm", gg_effective_emissions_comp)
}

################################################
# Plot the difference in practiced MIT,
# PT and Emissions.
################################################
if(exists("practiced_practices_mit_pt_co2_baseline") && exists("practiced_practices_mit_pt_co2_policy")) {
  practiced_practices_mit_pt_co2_baseline[, year:=seq(from=20, to=dim(practiced_practices_mit_pt_co2_baseline)[1]+19, by=1)]
  
  practiced_practices_comp <- rbind(practiced_practices_mit_pt_co2_baseline, practiced_practices_mit_pt_co2_policy)
  
  gg_pp_mit_comp <- ggplot(practiced_practices_comp, aes(x=year, y=MIT, colour=Scenario)) + 
    geom_line() +
    labs(colour = "Scenario", x = "Time", y = "MIT", title="A") +
    theme(plot.title = element_text(hjust = 0.5)) +
    theme(legend.position = "none") +
    scale_y_continuous(breaks=seq(0, 100, 5)) +
    scale_x_continuous(breaks=seq(0, 100, 10))
  print(gg_pp_mit_comp)
  
  gg_pp_pt_comp <- ggplot(practiced_practices_comp, aes(x=year, y=PT, colour=Scenario)) + 
    geom_line() +
    labs(colour = "Scenario", x = "Time", y = "PT", title="B") +
    theme(plot.title = element_text(hjust = 0.5)) +
    theme(legend.position = "bottom") +
    scale_y_continuous(breaks=seq(0, 100, 5)) +
    scale_x_continuous(breaks=seq(0, 100, 10))
  print(gg_pp_pt_comp)
  
  gg_pp_emissions_comp <- ggplot(practiced_practices_comp, aes(x=year, y=CO2, colour=Scenario)) + 
    geom_line() +
    labs(colour = "Scenario", x = "Time", y = "Emissions", title="A") +
    theme(plot.title = element_text(hjust = 0.5)) +
    theme(legend.position = "none") +
    scale_y_continuous(breaks=seq(0, 100, 5)) +
    scale_x_continuous(breaks=seq(0, 100, 10))
  print(gg_pp_emissions_comp)
  
  pp_mit_pt_comp <- grid.arrange(gg_pp_mit_comp, gg_pp_pt_comp, ncol=2)
  
  filename_to_be_saved <- paste0(output_directory, "pp_mit_pt_comp.png")
  # Save it
  ggsave(filename_to_be_saved, dpi = 300, width = 20, height = 10, units = "cm", pp_mit_pt_comp)
  
  
  pp_co2_ee_comp <- grid.arrange(gg_pp_emissions_comp, gg_effective_emissions_comp,  ncol=2)
  
  filename_to_be_saved <- paste0(output_directory, "pp_co2_ee_comp.png")
  # Save it
  ggsave(filename_to_be_saved, dpi = 300, width = 20, height = 10, units = "cm", pp_co2_ee_comp)
  
}
