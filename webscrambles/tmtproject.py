import tmt
import subprocess

class Project(tmt.EclipseProject):
    def configure(self):
        tmt.EclipseProject.configure(self)
        tmt.WinstoneServer.addPlugin(self)

        self.nonJavaResourceDeps |= tmt.glob(self.src, '.*html$', relativeTo=self.src)
        self.nonJavaResourceDeps |= tmt.glob(self.src, '.*ttf$', relativeTo=self.src)
        print(self.nonJavaResourceDeps)

Project(tmt.projectName(), description="A server plugin wrapper for scrambles that also draws pdfs.")
