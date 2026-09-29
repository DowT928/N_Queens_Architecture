/* Shared process/logging utility. AI: Codex generated; owner review pending. */
using System;
using System.Diagnostics;
using System.IO;
using System.Text;
using System.Threading.Tasks;

public static class NQueensCapture {
    private static async Task Pump(Stream input, FileStream output, TextWriter terminal) {
        byte[] buffer = new byte[8192];
        char[] chars = new char[8192];
        Decoder decoder = Encoding.UTF8.GetDecoder();
        int count;
        while ((count = await input.ReadAsync(buffer, 0, buffer.Length)) != 0) {
            await output.WriteAsync(buffer, 0, count);
            int length = decoder.GetChars(buffer, 0, count, chars, 0, false);
            terminal.Write(chars, 0, length);
            terminal.Flush();
        }
        int remaining = decoder.GetChars(buffer, 0, 0, chars, 0, true);
        terminal.Write(chars, 0, remaining);
    }
    public static int Run(string executable, string arguments, string cwd, string stdout, string stderr) {
        using (FileStream outFile = new FileStream(stdout, FileMode.CreateNew))
        using (FileStream errFile = new FileStream(stderr, FileMode.CreateNew))
        using (Process process = new Process()) {
            process.StartInfo = new ProcessStartInfo(executable, arguments) {
                WorkingDirectory = cwd, UseShellExecute = false, CreateNoWindow = true,
                RedirectStandardOutput = true, RedirectStandardError = true
            };
            process.Start();
            Task a = Pump(process.StandardOutput.BaseStream, outFile, Console.Out);
            Task b = Pump(process.StandardError.BaseStream, errFile, Console.Error);
            process.WaitForExit();
            Task.WaitAll(a, b);
            return process.ExitCode;
        }
    }
}
