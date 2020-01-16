#################################################
# Create five plots of effective emissions:     #                   
# 1.) Varying MIT (-1, 0, 1)                    #
# 2.) Varying PT (-1, 0, 1)                     #
# 3.) Varying CO2 (-1, 0, 1)                    #
# 4.) Varying Conv (-1, 0, 1)                   #
# 5.) Varying subsidies for BEV (-10%, 0 10%)   #
#################################################
## Repeat for every case
#technologies_output_baseline_mit_-1.csv
#technologies_output_baseline_mit_+1.csv
#technologies_output_baseline_pt_-1.csv
#technologies_output_baseline_pt_+1.csv
#technologies_output_baseline_co2_-1.csv
#technologies_output_baseline_co2_+1.csv
#technologies_output_baseline_conv_-1.csv
#technologies_output_baseline_conv_+1.csv

################################################
# Initialization
################################################
# Clear everything.
rm(list=ls())

# Load packages.
require("ggplot2")
require("data.table")
require("gridExtra")
require("Hmisc")
require("gdata")

# Set working directory.
setwd("C:/Users/moritz/Google\ Drive/RUB/Masterarbeit/MATISSE\ workspace/matisse/output/batch_output")

# Set the ouput directory.
output_directory <- "../../../../Latex/pictures/"

# Output list, which is used later on to clean up selectively.
ee_list <- c("ee_list", "case_list", "output_directory")

################################################
# Data operations                              #
################################################
# Set the cases.
case_list = c("baseline",
              "baseline_mit-1",
              "baseline_mit+1",
              "baseline_pt-1",
              "baseline_pt+1",
              "baseline_co2-1",
              "baseline_co2+1",
              "baseline_conv-1",
              "baseline_conv+1",
              "baseline_sub_10",
              "baseline_sub_-10")

# Loop through the cases.
for(case in case_list) {
  # Set the file name.
  filename_technologies_output <- paste0("technologies_output_batch_", case, ".csv")
  # Read the files.
  technologies_output <- fread(filename_technologies_output, header=TRUE, na.strings="null")
  
  # Drop last column, because it is empty.
  technologies_output[,dim(technologies_output)[2]:=NULL]
  
  # Function to replace NA values with 0. NA values exist, because those technologies were absorbed/clustered with.
  f_replace_na = function(DT) {
    for (j in names(DT))
      set(DT, which(is.na(DT[[j]])), j, 0)
  }
  
  # Drop NA values.
  f_replace_na(technologies_output)
  # Calculate the mean in each year for every column except for those containing the names and the states (Regime, EN, Niche).
  means_per_year <- technologies_output[, lapply(.SD, mean), by=year, .SDcols = !grepl("name|state", names(technologies_output))]
  # Drop the first column, because it is a duplicate.
  means_per_year[, 1:=NULL]
  # Drop the first twenty rows, because they are the burn-in phase.
  means_per_year <- means_per_year[year >= 20]
  
  # Drop direction and strength columns.
  practices_means <- means_per_year[, print(.SD), .SDcols = !grepl("direction|strength", names(means_per_year))]
  
  # Get the columns with the number of supporters.
  index_technology <- grep("number_supporters", names(practices_means))
  
  # Loop through all the columns with the number of supporters.
  for (k in index_technology) {
    # Get the name of the current technology.
    name_of_technology <- sub("number_supporters_", "", names(practices_means)[k])
    # Get the columns of all practices of this technology.
    index_practices <- grep(name_of_technology, names(practices_means))
    # Drop the column for number of supporters.
    index_practices <- index_practices[-(1)]
    # Loop through all the practices and multiply them with the
    # number of supporters.
    for(l in index_practices) {
      set(practices_means, i=NULL, j=l, value=practices_means[[k]]*practices_means[[l]])
    }
  }
  
  # Sum up all the rows.
  practices_means[, pp_CO2 := rowSums(.SD), .SDcols = grep("co2", names(practices_means))]
  practices_means[, pp_Cost := rowSums(.SD), .SDcols = grep("cost", names(practices_means))]
  practices_means[, pp_MIT := rowSums(.SD), .SDcols = grep("mit", names(practices_means))]
  practices_means[, pp_BE := rowSums(.SD), .SDcols = grep("be", names(practices_means))]
  practices_means[, pp_PT := rowSums(.SD), .SDcols = grep("pt", names(practices_means))]
  practices_means[, pp_Conv := rowSums(.SD), .SDcols = grep("conv", names(practices_means))]
  
  # Extract the year and the practices.
  practiced_practices_means <- practices_means[, print(.SD), .SDcols = grepl("year|pp_", names(practices_means))]
  
  # Rename columns.
  setnames(practiced_practices_means, sub("pp_","", names(practiced_practices_means)))
  
  # Store MIT, PT and Emissions for comparison.
  practiced_practices_mit_pt_co2 <- practiced_practices_means[, print(.SD), .SDcols = grepl("CO2|MIT|PT|year", names(practiced_practices_means))]
  practiced_practices_mit_pt_co2[, Scenario:=capitalize(case)]
  #assign(paste0("practiced_practices_mit_pt_co2_", case), practiced_practices_mit_pt_co2)
  
  effective_emissions <- practiced_practices_means
  set(effective_emissions, i=NULL, j=2L, value=effective_emissions[['CO2']]*(effective_emissions[['MIT']]+effective_emissions[['PT']]))
  effective_emissions <- effective_emissions[, print(.SD), .SDcols = grepl("year|CO2", names(effective_emissions))]
  
  # Set the column name.
  setnames(effective_emissions, "CO2", case)
  # Assign another name to save it for later.
  assign(paste0("effective_emissions_", case), effective_emissions)
  
  # Clean-up.
  ee_list <- c(ee_list, paste0("effective_emissions_", case))
  rm(list=setdiff(ls(), ee_list))
}

# Prepare data.table to bind everything together.
effective_emissions = data.table()
effective_emissions <- cbind(effective_emissions, effective_emissions_baseline[["year"]])
setnames(effective_emissions, "year")

# Bind everything together.
for(case in case_list) {
  tmp <- get(paste0("effective_emissions_", case))
  effective_emissions <- cbind(effective_emissions, tmp[[2]])
  setnames(effective_emissions, dim(effective_emissions)[2], case)
}

################################################
# Produce output                               #
################################################
# Plot MIT variations.
effective_emissions_mit_plot <- effective_emissions[, print(.SD), .SDcols = !grepl("pt|co2|conv|sub", names(effective_emissions))]
effective_emissions_mit_plot <- melt(effective_emissions_mit_plot, id="year")

gg_effective_emissions_mit_plot <- ggplot(effective_emissions_mit_plot, aes(x=year, y=value, color=variable))+
  labs(colour = "Variations", x = "Time", y = "Effective emissions", title="A") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "bottom") +
  scale_y_continuous(breaks=seq(0, 8000, 1000), limits = c(0, 8000)) +
  geom_line()

#print(gg_effective_emissions_mit_plot)

# Plot PT variations.
effective_emissions_pt_plot <- effective_emissions[, print(.SD), .SDcols = !grepl("mit|co2|conv|sub", names(effective_emissions))]
effective_emissions_pt_plot <- melt(effective_emissions_pt_plot, id="year")

gg_effective_emissions_pt_plot <- ggplot(effective_emissions_pt_plot, aes(x=year, y=value, color=variable))+
  labs(colour = "Variations", x = "Time", y = "Effective emissions", title="B") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "bottom") +
  scale_y_continuous(breaks=seq(0, 8000, 1000), limits = c(0, 8000)) +
  geom_line()

#print(gg_effective_emissions_pt_plot)

# Plot CO2 variations.
effective_emissions_co2_plot <- effective_emissions[, print(.SD), .SDcols = !grepl("mit|pt|conv|sub", names(effective_emissions))]
effective_emissions_co2_plot <- melt(effective_emissions_co2_plot, id="year")

gg_effective_emissions_co2_plot <- ggplot(effective_emissions_co2_plot, aes(x=year, y=value, color=variable))+
  labs(colour = "Variations", x = "Time", y = "Effective emissions", title="A") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "bottom") +
  scale_y_continuous(breaks=seq(0, 8000, 1000), limits = c(0, 8000)) +
  geom_line()

#print(gg_effective_emissions_co2_plot)

# Plot Conv variations.
effective_emissions_conv_plot <- effective_emissions[, print(.SD), .SDcols = !grepl("mit|pt|co2|sub", names(effective_emissions))]
effective_emissions_conv_plot <- melt(effective_emissions_conv_plot, id="year")

gg_effective_emissions_conv_plot <- ggplot(effective_emissions_conv_plot, aes(x=year, y=value, color=variable))+
  labs(colour = "Variations", x = "Time", y = "Effective emissions", title="B") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "bottom") +
  scale_y_continuous(breaks=seq(0, 8000, 1000), limits = c(0, 8000)) +
  geom_line()

#print(gg_effective_emissions_conv_plot)

# Plot subsidiy variations.
effective_emissions_subsidy_plot <- effective_emissions[, print(.SD), .SDcols = !grepl("mit|pt|co2|conv", names(effective_emissions))]
effective_emissions_subsidy_plot <- melt(effective_emissions_subsidy_plot, id="year")

gg_effective_emissions_subsidy_plot <- ggplot(effective_emissions_subsidy_plot, aes(x=year, y=value, color=variable))+
  labs(colour = "Variations", x = "Time", y = "Effective emissions") +
  theme(plot.title = element_text(hjust = 0.5)) +
  theme(legend.position = "bottom") +
  scale_y_continuous(breaks=seq(0, 8000, 1000), limits = c(0, 8000)) +
  geom_line()

print(gg_effective_emissions_subsidy_plot)


# Stitch everything together and save it
output_plot_1 <- grid.arrange(gg_effective_emissions_mit_plot, gg_effective_emissions_pt_plot,  ncol=1)
output_plot_2 <- grid.arrange(gg_effective_emissions_co2_plot, gg_effective_emissions_conv_plot, ncol=1)

# Save it
ggsave(paste0(output_directory, "sensitivity_analysis_mit_pt.png"), dpi = 300, width = 17.5, height = 25, units = "cm", output_plot_1)
ggsave(paste0(output_directory, "sensitivity_analysis_co2_conv.png"), dpi = 300, width = 17.5, height = 25, units = "cm", output_plot_2)
ggsave(paste0(output_directory, "sub_sens.png"), dpi = 300, width = 15, height = 15, units = "cm", gg_effective_emissions_subsidy_plot)

