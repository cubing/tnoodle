import tmt
import subprocess
from os.path import join, exists

class Project(tmt.EclipseProject):
	def configure(self):
		tmt.EclipseProject.configure(self)
		self.twophase_tables = join(self.binResource, 'twophase_tables')

	def compile(self):
		tmt.EclipseProject.compile(self)
		# build the pruning and transition tables for the two phase algorithm
		# iff they don't already exist
		if not exists(self.twophase_tables):
			print "Generating %s" % self.twophase_tables
			assert 0 == tmt.java(
							main="cs.min2phase.Tools",
							classpath=self.getClasspath(),
							args=[ self.twophase_tables ])
			print "Successfully generated %s!" % self.twophase_tables

Project(tmt.projectName(), description="A copy of Chen Shuang's (https://github.com/ChenShuang) awesome 3x3 scrambler built on top of Herbert Kociemba's Java library.")
