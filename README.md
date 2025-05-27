![Banner](https://i.imgur.com/hwRDyes.jpeg)

# 8b8tCore
*Folia Core Plugin for 8b8t and Anarchy Servers*
<p align="center">
  <img src="https://img.shields.io/github/stars/XeraPlugins/8b8tCore.svg?style=for-the-badge&label=Stars&color=yellow" alt="Stars" height="22">
  <img src="https://img.shields.io/github/forks/XeraPlugins/8b8tCore.svg?style=for-the-badge&label=Forks&color=blue" alt="Forks" height="22">
  <img src="https://img.shields.io/github/commit-activity/m/XeraPlugins/8b8tCore.svg?style=for-the-badge&label=Commits&color=orange" alt="Commits" height="22">
  <img src="https://img.shields.io/github/contributors/XeraPlugins/8b8tCore.svg?style=for-the-badge&label=Contributors&color=lightgrey" alt="Contributors" height="22">
  <img src="https://img.shields.io/github/release/XeraPlugins/8b8tCore.svg?style=for-the-badge&label=Latest%20Release&color=blue" alt="Latest Release" height="22">
</p>

## Features

- **Home Commands:** Set, delete and teleport to personal home locations.
- **Tpa Commands:** Send TPA requests to another player, accept or cancel them.
- **Help Command:** Provides assistance and command information.
- **Elytra Speed Limiter:** Controls the speed of players using Elytras.
- **Anti-Illegal:** Prevents illegal items such as bedrock, barrier blocks, end frames, etc.
- **Tablist:** Interface that displays a list of all connected players with a preview of their head and their current ping.
- **World Switching:** Allows operators to change worlds.
- **Entity Per Chunk Limit:** Controls entity limits per chunk, configurable per entity type.
- **Enhanced Chat:** Green text and private messaging similar to ChatCo., but with modern code improvements.
- **Language Locale:** Supports multiple languages for player messages.
- **Voting Integration:** Compatible with VotifierPlus for voting rewards. [VotifierPlus Github (Folia Support)](https://github.com/BenCodez/VotifierPlus)
- **Anti-Lag Machine:** Monitors and adjusts server performance based on TPS and MSPT, with configurable settings.
- **Per-Region TPS:** Displays TPS information in the tab list.
- **Auto Message:** Sends configurable auto messages at set intervals.
- **Death Messages:** Customizable death messages for players.
- **Permission-Based View and Simulation Distance:** Adjusts view and simulation distance based on player permissions.
- **/tps Command:** Provides TPS and MSPT metrics with permission control.
- **Custom Prefix:** Customize the prefix of messages according to your server.
- **Command Whitelist:** Select the commands that are allowed; the rest will be denied.
- **Op Security:** Whitelist of users who can be operators; if an external person is detected, their permission will be revoked.

## Available Languages

This plugin supports the following languages, which are displayed to each player according to their region.

<p align="center">
  <img src="https://flagcdn.com/w40/cn.png" alt="Chinese" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/gb.png" alt="English" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/es.png" alt="Spanish" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/fr.png" alt="French" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/it.png" alt="Italian" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/br.png" alt="Portuguese" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/ru.png" alt="Russian" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/sa.png" alt="Arabic" style="margin-right: 10px;">
  <img src="https://flagcdn.com/w40/in.png" alt="Hindi">
</p>


## To Do

- **Feature Suggestions:** We welcome your ideas for new features! Join our [Matrix room](https://matrix.to/#/#xera-general:matrix.xera.ca) to share your thoughts.

## Discussion / Support

<p align="center">
  <a href="https://matrix.to/#/#xera:matrix.xera.ca">
    <img src="https://img.shields.io/badge/Join_Matrix-7F8C8D?style=for-the-badge&logo=matrix&logoColor=white" alt="Join Matrix">
  </a>
</p>


## Installation

1. Download the latest version of the 8b8tCore plugin.
2. Place the plugin file into your server's `plugins` folder.
3. Restart your server to enable the plugin.
4. Configure the plugin settings in `config.yml` as needed.

## Compilation Guide

To compile and build the project, follow these steps:

### Prerequisites

1. **Java Development Kit (JDK) 21:** Ensure you have JDK 21 installed on your machine. You can download it from the [AdoptOpenJDK website](https://adoptium.net/) or the [Oracle website](https://www.oracle.com/java/technologies/javase/jdk21-archive-downloads.html).

2. **Maven:** Make sure Maven is installed. You can download it from the [Apache Maven website](https://maven.apache.org/download.cgi).

3. **IntelliJ IDEA:** Download and install IntelliJ IDEA from the [JetBrains website](https://www.jetbrains.com/idea/download/).

4. **Minecraft Dev Plugin:** Install the Minecraft Dev plugin for IntelliJ IDEA to help with Minecraft plugin development.

5. **Lombok:** Ensure Lombok is set up in your IntelliJ IDEA for better development experience.

### Building the Project

1. **Clone the Repository:**

   ```bash
   git clone https://github.com/XeraPlugins/8b8tCore.git
   ```
2. **Import the Project:**

    - Open IntelliJ IDEA.
    - Go to **File** > **Open** and select the cloned repository folder.
    - IntelliJ IDEA will detect the Maven project and prompt to import it. Click **Import**.

3. **Setup Project:**

    - **Install Dependencies:** IntelliJ IDEA will automatically download the required dependencies defined in the `pom.xml` file. If not, you can manually trigger this by right-clicking on the `pom.xml` file and selecting **Maven** > **Reload Project**.
    - **Enable Lombok:** Ensure Lombok is enabled by going to **File** > **Settings** > **Plugins**, searching for Lombok, and making sure it is installed and enabled.

4. **Compile the Project:**

    - **Using IntelliJ IDEA:**
        - Right-click on the project folder in the **Project** view.
        - Select **Maven** > **Package** to build the project.

    - **Using Command Line:**
        - Open a terminal or command prompt.
        - Navigate to the project directory.
        - Run the following command to compile and package the project:

          ```bash
          mvn clean package
          ```

### Notes

- Ensure that the Java SDK and Maven versions match the project's requirements.
- If you encounter any issues, refer to the project’s existing code and follow the coding standards used.
- Consult the [Maven documentation](https://maven.apache.org/guides/index.html) for additional Maven-related commands and configurations.

Feel free to reach out if you have any questions or run into issues!

## Contributing

- Contributions are welcome! If you have suggestions or want to contribute, please open a pull request or raise an issue on our GitHub repository.
- When contributing, please follow the existing code structure and programming style used in the project. This helps maintain consistency and makes it easier to review and integrate your changes.
- If you're unsure about the code structure or style, review the existing code and follow the patterns used. While there is no formal contribution guide, adhering to these practices will help ensure a smooth contribution process.

## License

This project is licensed under the [Unlicense](https://unlicense.org). This means that the software is dedicated to the public domain and can be freely used, modified, and distributed by anyone for any purpose. For more details, please refer to the [LICENSE](https://github.com/XeraPlugins/8b8tCore/blob/master/LICENSE) file in this repository.
